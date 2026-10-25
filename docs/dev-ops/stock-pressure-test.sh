#!/bin/bash
# ============================================================
# 防超卖并发压测脚本（教材四 实验 E2 / 待验证项 C-8）
#
# 设计思路：N 个测试用户各加购 1 件商品，把 Redis 可售库存压到 LIMIT(<N)，
# 同时并发下单。预期：成功订单数 == LIMIT，Redis 最终值 == LIMIT - 成功数 >= 0，
# 任何时刻不出现持久负库存（瞬时负值会被负数补偿机制回滚）。
#
# 前置条件：
#   1. 后端运行在 8092（mvn spring-boot:run）
#   2. MySQL/Redis 容器运行中
#   3. 注意：Redis 使用 1 号库（application-dev.yml database: 1）
#
# 用法：bash docs/dev-ops/stock-pressure-test.sh [并发用户数] [库存限制] [商品ID]
# ============================================================
set -u

BASE="http://localhost:8092/mall-api/v1"
USERS=${1:-20}
LIMIT=${2:-5}
PRODUCT_ID=${3:-1}
RUN_ID="${RANDOM}${RANDOM}"   # 本轮唯一标识，用户名与 DB 断言均以它为界，避免历史压测数据污染

RP=$(docker inspect s-pay-mall-redis --format '{{json .Config.Cmd}}' | sed -n 's/.*--requirepass","\([^"]*\)".*/\1/p')
MP=$(docker exec s-pay-mall-mysql printenv MYSQL_ROOT_PASSWORD)

echo "== 参数: 并发用户=$USERS, 库存上限=$LIMIT, 商品ID=$PRODUCT_ID =="

# ---- 1. 注册用户并获取 token（注册失败则尝试登录，幂等可重入） ----
echo "== 阶段1: 注册 $USERS 个测试用户 =="
tokens=()
for i in $(seq 1 "$USERS"); do
  u="pt_${RUN_ID}_${i}"
  resp=$(curl -s -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$u\",\"password\":\"pt123456\"}")
  tok=$(echo "$resp" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
  if [ -z "$tok" ]; then
    resp=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
      -d "{\"username\":\"$u\",\"password\":\"pt123456\"}")
    tok=$(echo "$resp" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
  fi
  if [ -n "$tok" ]; then
    tokens+=("$tok")
  else
    echo "  [警告] 用户 $u 注册/登录失败，跳过"
  fi
done
echo "  有效 token 数: ${#tokens[@]}"

# ---- 2. 每个用户加购 1 件目标商品（验证加购成功才进入压测） ----
echo "== 阶段2: 加购商品 $PRODUCT_ID =="
ok=0
for tok in "${tokens[@]}"; do
  resp=$(curl -s -X POST "$BASE/cart" -H "Authorization: Bearer $tok" -H 'Content-Type: application/json' \
    -d "{\"productId\":$PRODUCT_ID,\"quantity\":1}")
  echo "$resp" | grep -q '"code":"0000"' && ok=$((ok+1))
done
echo "  加购成功: $ok / ${#tokens[@]}"
if [ "$ok" -lt "$USERS" ]; then
  echo "  [警告] 部分用户加购失败，压测结果需按 DB 复核"
fi

# ---- 3. 把 Redis 可售库存压到 LIMIT ----
docker exec s-pay-mall-redis redis-cli -a "$RP" --no-auth-warning -n 1 SET "mall:product:stock:$PRODUCT_ID" "$LIMIT" >/dev/null
echo "== 阶段3: Redis 可售库存已设置为 $LIMIT =="

# ---- 4. 并发下单（每个请求唯一 requestId，否则幂等键会拦截重复请求） ----
echo "== 阶段4: ${#tokens[@]} 个请求并发下单 =="
rm -rf /tmp/pt-responses && mkdir -p /tmp/pt-responses
i=0
for tok in "${tokens[@]}"; do
  i=$((i+1))
  ( curl -s -X POST "$BASE/orders" -H "Authorization: Bearer $tok" -H 'Content-Type: application/json' \
      -d "{\"address\":\"压测地址\",\"requestId\":\"pt-req-${RANDOM}-$$-$i\"}" \
      > "/tmp/pt-responses/$i.txt" ) &
done
wait

# ---- 5. 统计与断言 ----
succ=$(grep -l '"code":"0000"' /tmp/pt-responses/*.txt 2>/dev/null | wc -l | tr -d ' ')
echo "== 阶段5: 结果统计 =="
echo "  下单成功响应: $succ / ${#tokens[@]}  (期望上限 $LIMIT)"
echo "  失败响应分布:"
grep -h '"code"' /tmp/pt-responses/*.txt | grep -v '"code":"0000"' | sort | uniq -c | sed 's/^/    /'
echo "  （STOCK_INSUFFICIENT = 扣成负数被补偿拒绝，StockGatewayImpl L45-49；0001 调用失败中的"
echo "   \"商品库存不足: productId=\" = 预检 hasEnoughStock 拒绝，MallOrderServiceImpl L49-52。均为正常拒绝路径）"

final=$(docker exec s-pay-mall-redis redis-cli -a "$RP" --no-auth-warning -n 1 GET "mall:product:stock:$PRODUCT_ID")
echo "  Redis 最终库存: $final  (期望 $((LIMIT - succ))，且 >= 0)"

db_items=$(docker exec s-pay-mall-mysql mysql -uroot -p"$MP" s-pay-mall -N -e \
  "SELECT COALESCE(SUM(oi.quantity),0) FROM order_main o JOIN order_item oi ON oi.order_id=o.id \
   WHERE o.user_id IN (SELECT id FROM mall_user WHERE username LIKE 'pt_${RUN_ID}\\_%');" 2>/dev/null)
echo "  DB 中压测用户订单的总件数: $db_items  (期望 == 成功订单数，且 <= $LIMIT)"

# ---- 6. 恢复环境：Redis 库存与 DB 对齐 ----
db_stock=$(docker exec s-pay-mall-mysql mysql -uroot -p"$MP" s-pay-mall -N -e \
  "SELECT stock FROM product WHERE id=$PRODUCT_ID;" 2>/dev/null)
docker exec s-pay-mall-redis redis-cli -a "$RP" --no-auth-warning -n 1 SET "mall:product:stock:$PRODUCT_ID" "$db_stock" >/dev/null
echo "== 阶段6: Redis 库存已恢复 = DB 账面库存 $db_stock =="

# ---- 断言结论 ----
echo "== 结论 =="
if [ "$succ" -le "$LIMIT" ] && [ "$final" -ge 0 ] && [ "$db_items" -eq "$succ" ]; then
  echo "  防超卖验证通过：成功 $succ <= 库存 $LIMIT，Redis 无负值，DB 订单与成功数一致"
  exit 0
else
  echo "  验证未通过：请检查上述数字"
  exit 1
fi

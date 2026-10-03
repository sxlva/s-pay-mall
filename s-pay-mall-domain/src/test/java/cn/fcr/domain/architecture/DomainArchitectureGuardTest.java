package cn.fcr.domain.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.tngtech.archunit.lang.conditions.ArchConditions.haveRawReturnType;
import static com.tngtech.archunit.lang.conditions.ArchConditions.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * DDD 领域层架构边界自动化守卫测试
 *
 * @author 傅崇睿
 */
@DisplayName("DDD 领域层架构边界自动化守卫测试")
public class DomainArchitectureGuardTest {

    private static JavaClasses domainClasses;

    @BeforeAll
    public static void setup() {
        // 导入当前 domain 模块下的所有生产代码，排除测试代码本身
        domainClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("cn.fcr.domain..");
    }

    @Test
    @DisplayName("守卫规则 1: 严禁在领域层出现以 Port 结尾的接口或类")
    public void domain_layer_should_not_have_port_naming() {
        ArchRule rule = classes()
                .that().resideInAPackage("..domain..")
                .should().haveSimpleNameNotEndingWith("Port")
                .because("根据最新六边形架构规范，外部系统交互契约应统一命名为 Gateway 并放置于 gateway 包下");

        rule.check(domainClasses);
    }

    @Test
    @DisplayName("守卫规则 2: 领域层 Repository 接口的方法严禁返回原生 Map")
    public void repository_methods_should_not_return_map() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().resideInAPackage("..domain..adapter.repository..")
                .should(not(haveRawReturnType(Map.class)))
                .because("基础设施层数据必须通过防腐层(ACL)转换为强类型 Entity/VO，禁止使用 Map 破坏强类型契约");

        rule.check(domainClasses);
    }

    @Test
    @DisplayName("守卫规则 3: mall 域只允许依赖 order 域的 gateway 接口，严禁依赖订单实体/服务/读模型")
    public void mall_should_only_depend_on_order_gateway() {
        // 白名单：order.gateway 包下的接口是跨域唯一合法出口
        ArchRule allowGateway = noClasses()
                .that().resideInAPackage("..domain.mall..")
                .should().dependOnClassesThat(resideInAPackage("..domain.order..")
                        .and(notInAPackage("..domain.order.gateway..")))
                .as("mall 对 order 的依赖仅限 gateway 包")
                .because("跨域只准通过 gateway 接口，不准互相 import 实体/服务/读模型");

        allowGateway.check(domainClasses);
    }

    @Test
    @DisplayName("守卫规则 4: order 域严禁依赖 mall 域的商品/购物车/用户实体与服务（库存网关与购物车读模型除外）")
    public void order_should_not_depend_on_mall_business() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.order..")
                .should().dependOnClassesThat(resideInAPackage("..domain.mall..")
                        .and(notInAPackage("..domain.mall..gateway.."))
                        .and(notInAPackage("..domain.mall.cart.model.valobj..")))
                .as("order 不再出现商品/购物车/用户类")
                .because("核心原则：order 管订单，依赖 mall 仅限库存网关接口与 CartItemVO 下单读模型（2026-10-03 M2 边界落地）");

        rule.check(domainClasses);
    }

    private static com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> resideInAPackage(String packageIdentifier) {
        return com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage(packageIdentifier);
    }

    private static com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> notInAPackage(String packageIdentifier) {
        return com.tngtech.archunit.base.DescribedPredicate.not(resideInAPackage(packageIdentifier));
    }
}

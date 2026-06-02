package cn.fcr.domain.order.adapter.port;

public interface IOrderEventPort {

    void sendDelayCloseMessage(String orderNo);

    void sendPaySuccessMessage(String orderNo);
}
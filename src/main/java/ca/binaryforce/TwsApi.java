package ca.binaryforce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.ib.client.Contract;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.ILiveOrderHandler;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ApiController.IPositionHandler;
import lombok.AllArgsConstructor;
import lombok.Data;

public class TwsApi implements IConnectionHandler, IPositionHandler, ILiveOrderHandler, IOrderHandler {

    private ApiController apiController;
    private Semaphore semaphore;

    private String host;
    private int port;
    private int clientId;

    private List<Position> positions;
    private List<Order> orders;

    public TwsApi(String host, int port, int clientId) {
        apiController = new ApiController(this, new TwsApi.TwsLogger("in"), new TwsApi.TwsLogger("out"));
        semaphore = new Semaphore(0);

        positions = new ArrayList<>();
        orders = new ArrayList<>();

        this.host = host;
        this.port = port;
        this.clientId = clientId;
    }

    public List<Position> getPositions() {
        positions.clear();

        connect();
        semaphore.drainPermits();
        apiController.reqPositions(this);
        if (!block()) {
            return null;
        }
        return positions;
    }

    public List<Order> getOrders() {
        orders.clear();

        connect();
        apiController.reqLiveOrders(this);
        block();
        return orders;
    }

    public void cancelAllOrders() {
        connect();
        apiController.cancelAllOrders();
    }

    public void submitOrder(Contract contract, com.ib.client.Order order) {
        order.clientId(clientId);
        apiController.placeOrModifyOrder(contract, order, this);
    }

    public boolean connect() {
        boolean result = true;
        if (!apiController.client().isConnected()) {
            semaphore.drainPermits();
            apiController.connect(host, port, clientId, "");
            result = block();
        }
        return result;
    }

    public boolean disconnect() {
        boolean result = true;
        if (apiController.client().isConnected()) {
            semaphore.drainPermits();
            apiController.disconnect();
            result = block();
        }
        return result;
    }

    @Override
    public void connected() {
        semaphore.release();
        ;
    }

    @Override
    public void disconnected() {
        semaphore.release();

    }

    private boolean block() {
        boolean result;
        try {
            result = semaphore.tryAcquire(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    @Override
    public void accountList(List<String> list) {
        // nothing
    }

    @Override
    public void error(Exception e) {
        // nothing
    }

    @Override
    public void message(int id, int errorCode, String errorMsg) {
        // nothing
    }

    @Override
    public void show(String string) {
        System.out.println("show: " + string);
    }

    @Override
    public void position(String account, Contract contract, double pos, double avgCost) {
        positions.add(new Position(account, contract, pos, avgCost));
    }


    @Override
    public void positionEnd() {
        semaphore.release();
    }

    @Override
    public void orderState(OrderState orderState) {

    }

    @Override
    public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld,
        double mktCapPrice) {
        System.out.println("orderStatus: " + status);
    }

    @Override
    public void handle(int errorCode, String errorMsg) {
        // nothing
    }

    @Override
    public void openOrder(Contract contract, com.ib.client.Order order, OrderState orderState) {
        orders.add(new Order(contract, order, orderState));
    }

    @Override
    public void openOrderEnd() {
        semaphore.release();
    }

    @Override
    public void orderStatus(int orderId, OrderStatus status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId,
        String whyHeld, double mktCapPrice) {

    }

    @Override
    public void handle(int orderId, int errorCode, String errorMsg) {
        // nothing
    }

    @AllArgsConstructor
    @Data
    public class Position {
        private String account;
        private Contract contract;
        private double quantity;
        private double avgCoast;
    }

    @AllArgsConstructor
    @Data
    public class Order {
        private Contract contract;
        private com.ib.client.Order order;
        private OrderState orderState;
    }

    private class TwsLogger implements ILogger {

        private String type;

        public TwsLogger(String tt) {
            type = tt;
        }

        @Override
        public void log(String valueOf) {
            System.out.println(valueOf);
        }
    }
}

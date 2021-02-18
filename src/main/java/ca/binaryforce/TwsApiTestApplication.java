package ca.binaryforce;

import java.util.List;

public class TwsApiTestApplication {
    public static void main(String[] args) {
        System.out.println("Hello world!");

        TwsApi twsApi = new TwsApi("", 7496, 1);
        twsApi.connect();
        List<TwsApi.Position> positions = twsApi.getPositions();
        positions.forEach((pp) -> System.out.println("in Positions: " + pp.getContract().symbol()));
        twsApi.disconnect();
    }
}

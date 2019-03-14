package trader.spread;

import com.trader.client.EventClientEndpoint;

public class SpreadMain {
	public static void main(String[] args) {
		System.out.println("version: " + 1);
		SpreadProps sp = new SpreadProps();
		SpreadTrader t = new SpreadTrader(sp);
		EventClientEndpoint.startClient(sp.market);

		// SpreadPricing pricing = new SomeSpread(0.1, 0.01);

		// LunoBTCManager m = new LunoBTCManager(EMarketType.EUR_BTC, pricing, 0.01,
		// 10);
		// m.tradeBTC(-0.005);

		EventClientEndpoint.waitIndefinitely();
	}

}

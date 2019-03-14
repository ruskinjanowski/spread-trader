package trader.spread;

import java.io.File;

import org.knowm.xchange.currency.CurrencyPair;

import com.trader.level.SomeSpread;
import com.trader.logging.LimitsAndRates;
import com.trader.logging.LoggingUtil;
import com.trader.logging.Transaction;
import com.trader.market.data.MarketData;
import com.trader.market.data.MarketData.MarketPrice;
import com.trader.single.AccWallet;
import com.trader.single.IOrderFilled;
import com.trader.single.LunoBTCManager;
import com.trader.single.OrderTracker;

import arbtrader.controller.MarketEvents;
import arbtrader.controller.MarketEvents.ISpreadListener;
import arbtrader.controller.MarketEvents.ReceivePriority;
import arbtrader.credentials.TraderFolders;
import arbtrader.credentials.TraderFolders.ProgramName;
import arbtrader.model.SpreadChanged;

public class SpreadTrader {
	private final File transactionFile;
	private final File rateFile;// date, rate lower, rate upper

	final LunoBTCManager lunoASK;
	final LunoBTCManager lunoBID;

	SpreadProps state;
	SomeSpread pricing;
	AccWallet wallet;

	public SpreadTrader(SpreadProps state) {
		this.state = state;
		pricing = new SomeSpread(state.market, 0.1, 0.01);// TODO
		// logging files
		transactionFile = new File(TraderFolders.getLogging(ProgramName.SpreadTrader), "transactions.txt");
		rateFile = new File(TraderFolders.getLogging(ProgramName.SpreadTrader), "rates.txt");

		wallet = new AccWallet(state.market);
		lunoASK = new LunoBTCManager(state.market, wallet);
		lunoASK.addOrderFilledListener(new OrderFilledASK());

		lunoBID = new LunoBTCManager(state.market, wallet);
		lunoBID.addOrderFilledListener(new OrderFilledBID());

		MarketEvents.get(state.market).addSpreadListener(new SpreadChangedTr(), ReceivePriority.HIGH);
	}

	private class OrderFilledASK implements IOrderFilled {

		@Override
		public void orderFilled(OrderTracker t) {

			Transaction tr = new Transaction(t.o.id, t.getFill(), t.o.price, t.orderType, CurrencyPair.BTC_EUR);
			LoggingUtil.appendToFile(transactionFile, tr.toString());
			System.out.println("ask order filled");
			System.out.println(tr.toString());
		}

	}

	private class OrderFilledBID implements IOrderFilled {

		@Override
		public void orderFilled(OrderTracker t) {
			Transaction tr = new Transaction(t.o.id, t.getFill(), t.o.price, t.orderType, CurrencyPair.BTC_EUR);
			LoggingUtil.appendToFile(transactionFile, tr.toString());
			System.out.println("bid order filled");
			System.out.println(tr.toString());
		}

	}

	private class SpreadChangedTr implements ISpreadListener {

		@Override
		public void spreadChanged() {
			evaluatePrices();
		}

	}

	synchronized void evaluatePrices() {

		SpreadChanged actual = MarketEvents.getSpread(state.market);
		SpreadChanged wanted = getLimits();
		double diff = Math.rint((actual.priceAsk - actual.priceBid) / (actual.priceBid) * 100 * 100000) / 100000.0;

		System.out.println("diff: " + diff + " actual: " + actual.priceAsk + "," + actual.priceBid + " wanted: "
				+ wanted.priceAsk + "," + wanted.priceBid);

		if (actual.priceAsk > wanted.priceAsk + 2 * pricing.inc) {// symmetric for bid
			System.out.println("Trading...");
			// trade
			MarketPrice bs = MarketData.INSTANCE.getUERrBTC(1);
			if (bs.ask < actual.priceAsk) {
				lunoASK.setWantedBTC(0);
			} else {
				System.out.println("ask blocked..." + bs.ask);
				lunoASK.setWantedBTC(wallet.getBtc());
			}
			if (bs.bid > actual.priceBid) {
				if (wallet.getBtc() == lunoBID.getWantedBTC()) {
					// this number fluctuates with price change so don't reset
					double buy = lunoBID.getWallet().getMaxBuy(actual.priceAsk);
					System.out.println("buy btc: " + buy);
					if (buy > 0.001) {
						lunoBID.tradeBTC(buy);
					}
				}
			} else {
				System.out.println("bid blocked..." + bs.bid);
				lunoBID.setWantedBTC(wallet.getBtc());
			}
		} else if (actual.priceAsk < wanted.priceAsk) {
			// do not trade
			System.out.println("Not trading...");
			lunoASK.setWantedBTC(wallet.getBtc());
			lunoBID.setWantedBTC(wallet.getBtc());
		} else {
			System.out.println("Hysteresis...");
			// Hysteresis, no change in state
		}
		writeToRateFile();

	}

	long lastWrite = 0;

	private void writeToRateFile() {
		if (System.currentTimeMillis() - lastWrite > 60_000) {
			lastWrite = System.currentTimeMillis();
			SpreadChanged actual = MarketEvents.getSpread(state.market);
			SpreadChanged wanted = getLimits();
			LimitsAndRates lr = new LimitsAndRates(actual.priceAsk, actual.priceBid, wanted.priceAsk, wanted.priceBid);
			LoggingUtil.appendToFile(rateFile, lr.toString());
		}
	}

	private SpreadChanged getLimits() {
		SpreadChanged s = MarketEvents.getSpread(state.market);
		double mid = (s.priceAsk + s.priceBid) / 2;
		double margin_perc = state.margin_perc / 2;
		return new SpreadChanged(mid * (1 + margin_perc / 100), mid * (1 - margin_perc / 100));
	}

}

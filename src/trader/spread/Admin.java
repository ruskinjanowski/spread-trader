package trader.spread;

import org.knowm.xchange.currency.Currency;

import com.trader.market.data.AccountData;

import arbtrader.credentials.EMarketType;

public class Admin {

	public static void main(String[] args) {
		/** AccountData */
		System.out.println(AccountData.getBalance(EMarketType.ZAR_BTC, Currency.ZAR, 1));
		System.out.println(AccountData.getBalance(EMarketType.ZAR_BTC, Currency.ZAR, 1));
		/** Send */
		// BitstampTrading.placeOrder(0.01);

		// BitstampTrading.sendToLuno(0.9938);
	}
}

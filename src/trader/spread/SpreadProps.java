package trader.spread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;

import arbtrader.credentials.EMarketType;
import arbtrader.credentials.TraderFolders;
import arbtrader.credentials.TraderFolders.ProgramName;

public class SpreadProps {
	private final File file;
	private final PropertiesConfiguration props = new PropertiesConfiguration();
	private final PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout(props);

	private double btc;
	private double counter;
	private final double tradeAmountBTC;
	public final double margin_perc;
	public final EMarketType market;

	/**
	 * 
	 * @param id
	 *            state for different trading strategies, diff, exchange rate
	 */
	public SpreadProps() {
		file = new File(TraderFolders.getConfig(ProgramName.SpreadTrader), "state.properties");

		try {
			layout.load(new InputStreamReader(new FileInputStream(file)));
		} catch (ConfigurationException | FileNotFoundException e) {
			e.printStackTrace();
		}

		btc = getProperty("btc");
		counter = getProperty("counter");
		tradeAmountBTC = getProperty("tradeAmountBTC");
		margin_perc = getProperty("margin_perc");
		market = null;// EMarketType.valueOf(getPropertyS("market"));
	}

	public void setAll(double lunoEURBTC, double availableEUR) {

		this.btc = lunoEURBTC;

		props.setProperty("lunoEURBTC", lunoEURBTC);
		props.setProperty("availableEUR", availableEUR);

		try {
			layout.save(new FileWriter(file, false));
		} catch (ConfigurationException | IOException e) {
			e.printStackTrace();
		}
	}

	public double getLunoEURBTC() {
		return btc;
	}

	public double getAvailableEUR() {
		return counter;
	}

	public double getTradeAmountBTC() {
		return tradeAmountBTC;
	}

	private double getProperty(String p) {
		String val = (String) props.getProperty(p);
		if (val == null) {
			throw new IllegalStateException(p);
		}
		return Double.parseDouble(val);
	}

	private String getPropertyS(String p) {
		String val = (String) props.getProperty(p);
		if (val == null) {
			throw new IllegalStateException(p);
		}
		return val;
	}
}

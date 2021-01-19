package omg.lol.jplexer.stocks;

import net.milkbowl.vault.economy.Economy;
import omg.lol.jplexer.stocks.commands.StocksCommand;
import omg.lol.jplexer.stocks.events.TimeSchedulerEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Stocks extends JavaPlugin {
    public Economy economy;
    Connection connection;

    Float currentSeed = null;

    @Override
    public void onDisable() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        economy = this.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        prepareDatabase();
        System.out.println(" ____  _____  ____  ____  _  __ ____ \n" +
                            "/ ___\\/__ __\\/  _ \\/   _\\/ |/ // ___\\\n" +
                            "|    \\  / \\  | / \\||  /  |   / |    \\\n" +
                            "\\___ |  | |  | \\_/||  \\_ |   \\ \\___ |\n" +
                            "\\____/  \\_/  \\____/\\____/\\_|\\_\\\\____/");
        this.getServer().getScheduler().runTaskTimer(this, new TimeSchedulerEvent(this), 0, 200);
        this.getCommand("stocks").setExecutor(new StocksCommand(this));
    }

    private void prepareDatabase() {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:stocks.db");
            this.connection.createStatement().execute("PRAGMA foreign_keys=ON");
            this.connection.createStatement().execute("CREATE TABLE IF NOT EXISTS lastDay(day INTEGER PRIMARY KEY)");
            this.connection.createStatement().execute("CREATE TABLE IF NOT EXISTS stockPrices(id INTEGER PRIMARY KEY AUTOINCREMENT, seed FLOAT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getDatabase() {
        return connection;
    }
    public static String getDollarValue(int cents) { return (cents < 0 ? "-" : "") + "$" + Math.abs(cents / 100) + "." + String.format("%02d", Math.abs(cents % 100)); }

    public void setCurrentSeed(float seed) {
        this.currentSeed = seed;
    }

    public Float currentSeed() {
        return this.currentSeed;
    }

    public int getBuyPrice() {
        return getBuyPrice(this.currentSeed);
    }

    public int getSellPrice() {
        return getSellPrice(this.currentSeed);
    }

    public int getBuyPrice(float seed) {
        return (int) (7400 * seed + 100 + (seed * 100));
    }

    public int getSellPrice(float seed) {
        return (int) (7400 * seed + 100 - (seed * 100));
    }
}

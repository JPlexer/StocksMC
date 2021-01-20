package omg.lol.jplexer.stocks.events;

import omg.lol.jplexer.stocks.Stocks;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TimeSchedulerEvent implements Runnable {
    Stocks plugin;
    Connection connection;

    long lastDay = -1;
    public TimeSchedulerEvent(Stocks plugin) {
        this.plugin = plugin;
        connection = plugin.getDatabase();

        try {
            ResultSet results = connection.createStatement().executeQuery("SELECT day FROM lastDay");
            if (results.next()) {
                lastDay = results.getInt("day");
            }
            iterateStocks();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        World world = plugin.getServer().getWorld("world");
        long currentDay = world.getFullTime() / 24000;
        if (currentDay != lastDay && plugin.getConfig().getBoolean("autoiterate")) iterateStocks();
        if (plugin.getConfig().getBoolean("pricejustset")) iterateStocksNew();
    }

    void iterateStocks() {
        try {
            World world = plugin.getServer().getWorld("world");
            lastDay = world.getFullTime() / 24000;

            //Add a new entry to the stock market
            Float lastSeed = plugin.currentSeed();
            if (lastSeed == null) {
                //Only bother the database if we're starting up
                ResultSet results = connection.createStatement().executeQuery("SELECT seed FROM stockPrices ORDER BY id DESC LIMIT 1");
                if (results.next()) {
                    lastSeed = results.getFloat("seed");
                } else {
                    lastSeed = 0.5f;
                }
            }

            float newSeed = (float) ((Math.random() / 10) - 0.05 + lastSeed);
            if (newSeed < 0) newSeed = 0;
            if (newSeed > 1) newSeed = 1;

            PreparedStatement statement = connection.prepareStatement("INSERT INTO stockPrices(seed) VALUES(?)");
            statement.setFloat(1, newSeed);
            statement.executeUpdate();

            plugin.setCurrentSeed(newSeed);
            writeLastDay();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    void iterateStocksNew() {
        try {
            World world = plugin.getServer().getWorld("world");
            lastDay = world.getFullTime() / 24000;

            //Add a new entry to the stock market
            Float lastSeed = plugin.currentSeed();
            if (lastSeed == null) {
                //Only bother the database if we're starting up
                ResultSet results = connection.createStatement().executeQuery("SELECT seed FROM stockPrices ORDER BY id DESC LIMIT 1");
                if (results.next()) {
                    lastSeed = results.getFloat("seed");
                } else {
                    lastSeed = 0.5f;
                }
            }

            float newSeed = (float) plugin.getConfig().get("seed");

            PreparedStatement statement = connection.prepareStatement("INSERT INTO stockPrices(seed) VALUES(?)");
            statement.setFloat(1, newSeed);
            statement.executeUpdate();

            plugin.setCurrentSeed(newSeed);
            plugin.getConfig().set("pricejustset", false);
            plugin.saveConfig();
            writeLastDay();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void writeLastDay() throws SQLException {
        connection.createStatement().executeUpdate("DELETE FROM lastDay");

        PreparedStatement statement = connection.prepareStatement("INSERT INTO lastDay(day) VALUES(?)");
        statement.setLong(1, lastDay);
        statement.executeUpdate();
    }
}

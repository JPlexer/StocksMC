package omg.lol.jplexer.stocks.commands;

import omg.lol.jplexer.stocks.Stocks;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import net.milkbowl.vault.economy.Economy;

import javax.swing.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class StocksCommand implements CommandExecutor {
    Stocks plugin;
    Connection connection;
    public StocksCommand(Stocks plugin) {
        this.plugin = plugin;

        connection = plugin.getDatabase();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        try {
            if (strings.length == 0) {
                viewStocks(commandSender);
                return true;
            }

            switch (strings[0]) {
                case "view":
                    viewStocks(commandSender);
                    return true;
                case "buy":
                    buyStocks(commandSender, strings);
                    return true;
                case "sell":
                    sellStocks(commandSender, strings);
                    return true;
                default:
                    commandSender.sendMessage(ChatColor.RED + "Sorry, \"" + strings[0] + "\" is not a valid verb. You can use view, buy or sell.");
                    return true;
            }
        } catch (SQLException exception) {
            commandSender.sendMessage(ChatColor.RED + "Sorry, a database error occurred. Contact an admin immediately!");
            return true;
        }
    }

    void viewStocks(CommandSender commandSender) throws SQLException {
        ArrayList<Float> seeds = new ArrayList<Float>();
        ResultSet results = connection.createStatement().executeQuery("SELECT seed FROM stockPrices ORDER BY id DESC LIMIT 5");
        while (results.next()) {
            seeds.add(results.getFloat("seed"));
        }

        commandSender.sendMessage(ChatColor.GREEN + "--- Current Stock Prices ---");
        commandSender.sendMessage(ChatColor.GREEN + "buy / sell");
        for (int i = 0; i < seeds.size(); i++) {
            float seed = seeds.get(i);
            int buyPrice = plugin.getBuyPrice(seed);
            int sellPrice = plugin.getSellPrice(seed);

            Float nextSeed = null;
            if (seeds.size() > i + 1) {
                nextSeed = seeds.get(i + 1);
            }

            ChatColor buyCol, sellCol;
            if (nextSeed == null || nextSeed == seed) {
                buyCol = ChatColor.WHITE;
                sellCol = ChatColor.WHITE;
            } else if (nextSeed > seed) {
                buyCol = ChatColor.AQUA;
                sellCol = ChatColor.RED;
            } else {
                buyCol = ChatColor.RED;
                sellCol = ChatColor.AQUA;
            }

            commandSender.sendMessage(buyCol + Stocks.getDollarValue(buyPrice) + ChatColor.WHITE + " / " + sellCol + Stocks.getDollarValue(sellPrice));
            if (i == 0) {
                commandSender.sendMessage(ChatColor.GREEN + "--- Historic Stock Prices ---");
            }
        }
    }

    void buyStocks(CommandSender commandSender, String[] strings) throws SQLException {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "Sorry, only players can participate in the stock market.");
            return;
        }

        if (strings.length != 2) {
            commandSender.sendMessage("Buy some emeralds from the stock market");
            commandSender.sendMessage("Usage: /stocks buy (amount)");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(strings[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            commandSender.sendMessage(ChatColor.RED + "Enter a valid amount of emeralds to purchase from the stock market.");
            return;
        }

        int buyPrice = plugin.getBuyPrice();
        int totalPrice = buyPrice * amount;

        double playerBalance = plugin.economy.getBalance((org.bukkit.OfflinePlayer) commandSender) * 100;
        int playerBal = (int) playerBalance;
        if (playerBal < totalPrice) {
            commandSender.sendMessage(ChatColor.RED + "Sorry, your balance is too low to complete that purchase.");
            commandSender.sendMessage(ChatColor.RED + "You need an additional " + Stocks.getDollarValue(totalPrice - playerBal) + " to complete this purchase.");
            return;
        }

        ItemStack itemStack = new ItemStack(Material.EMERALD, amount);
        PlayerInventory inventory = ((Player) commandSender).getInventory();
        HashMap<Integer, ItemStack> results = inventory.addItem(itemStack);
        if (results.size() != 0) {
            //Rollback
            ItemStack remaining = results.get(0);
            remaining.setAmount(amount - remaining.getAmount());
            inventory.removeItem(remaining);

            commandSender.sendMessage(ChatColor.RED + "Sorry, you don't have enough inventory space to complete that purchase.");
            return;
        }

            double totalPriceD = (double) totalPrice / 100;
            plugin.economy.withdrawPlayer((org.bukkit.OfflinePlayer) commandSender, totalPriceD);
            commandSender.sendMessage(ChatColor.GREEN + "Thanks for your purchase!");
            commandSender.sendMessage(ChatColor.GREEN + "Buy rate: " + Stocks.getDollarValue(buyPrice) + " ea.");
            commandSender.sendMessage(ChatColor.GREEN + "Purchase Price: " + Stocks.getDollarValue(totalPrice));
            commandSender.sendMessage(ChatColor.GREEN + "Emeralds Traded: " + amount + " emeralds");

    }

    void sellStocks(CommandSender commandSender, String[] strings) throws SQLException {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "Sorry, only players can participate in the stock market.");
            return;
        }

        if (strings.length != 2) {
            commandSender.sendMessage("Sell some emeralds to the stock market");
            commandSender.sendMessage("Usage: /stocks sell (amount)");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(strings[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            commandSender.sendMessage(ChatColor.RED + "Enter a valid amount of emeralds to sell to the stock market.");
            return;
        }

        int sellPrice = plugin.getSellPrice();
        int totalPrice = sellPrice * amount;

        ItemStack itemStack = new ItemStack(Material.EMERALD, amount);
        PlayerInventory inventory = ((Player) commandSender).getInventory();
        HashMap<Integer, ItemStack> results = inventory.removeItem(itemStack);
        if (results.size() != 0) {
            //Rollback
            ItemStack remaining = results.get(0);
            remaining.setAmount(amount - remaining.getAmount());
            inventory.addItem(remaining);

            commandSender.sendMessage(ChatColor.RED + "Sorry, you don't have enough emeralds to complete that purchase.");
            return;
        }
        double totalPriceD = (double) totalPrice / 100 ;
        plugin.economy.depositPlayer((org.bukkit.OfflinePlayer) commandSender, totalPriceD);
        commandSender.sendMessage(ChatColor.GREEN + "Thanks for your trade!");
        commandSender.sendMessage(ChatColor.GREEN + "Sell rate: " + Stocks.getDollarValue(sellPrice) + " ea.");
        commandSender.sendMessage(ChatColor.GREEN + "Total Price: " + Stocks.getDollarValue(totalPrice));
        commandSender.sendMessage(ChatColor.GREEN + "Emeralds Traded: " + amount + " emeralds");
    }
}

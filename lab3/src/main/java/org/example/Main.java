package org.example;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        String botToken = System.getenv("bot_token");

        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(botToken, new AppBot(botToken));
            System.out.println("WebLab3Bot successfully started!");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
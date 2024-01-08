package io.ussop.ExchangeRateProject.service;


import io.ussop.ExchangeRateProject.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.Current;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.money.MonetaryAmount;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.MonetaryConversions;
import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;

    private Map<Long, String> awaitingRates = new HashMap<>();
    private Map<Long, String> firstRates = new HashMap<>();

    // Enum для представления состояний бота

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> botCommandList = new ArrayList<>();
        botCommandList.add(new BotCommand("/start", "Start session with bot"));
        botCommandList.add(new BotCommand("/exchange", "exchange rate"));
        botCommandList.add(new BotCommand("/help", "helping button"));
        try {
            this.execute(new SetMyCommands(botCommandList, new BotCommandScopeDefault(),null));

        } catch (TelegramApiException e) {
            log.error("Error setting bots command list " + e.getMessage());
        }
    }
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

//            switch (messageText) {
//                case "/start":
//                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
//                    break;
//                case "/exchange":
//                    String answer = "Please, " + update.getMessage().getChat().getFirstName() + " write 2 rate for exchanging!";
//                    prepareAndSendMessage(chatId, answer);
//
//                    break;
//                default:
//                    prepareAndSendMessage(chatId, "Sorry, but Bot does not recognize this command!");
//                    break;
//            }
              if (awaitingRates.containsKey(chatId)) {
                  handleAwaitingRate(chatId, messageText);
              } else {
                 handleDefaultState(chatId, messageText, update);
              }

        }
    }

    private void handleAwaitingRate(long chatId, String messageText) {
        String awaitingKey = awaitingRates.get(chatId);

        if ("first".equals(awaitingKey)) {
            String firstRate = messageText;

            firstRates.put(chatId, firstRate);
            awaitingRates.put(chatId, "second");
            prepareAndSendMessage(chatId, "Please, enter the second currency rate:");
        } else if ("second".equals(awaitingKey)) {
            String secondRate = messageText;

            awaitingRates.remove(chatId);
            exchangeRate(chatId, firstRates.get(chatId), secondRate);
        }
    }


    private void handleDefaultState(long chatId, String messageText, Update update) {
        switch (messageText) {
            case "/start":
                startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                break;
            case "/exchange":
                // Инициируем ожидание первой валюты
                awaitingRates.put(chatId, "first");
                prepareAndSendMessage(chatId, "Please, enter the first currency rate:");
                break;
            default:
                prepareAndSendMessage(chatId, "Sorry, but Bot does not recognize this command!");
                break;
        }
    }

    private void startCommandReceived(long chatId, String firstName) {
        String answer = "Welcome to us, " + firstName + "! Here we provide current exchange rates and the ability to convert different currencies";

        log.info("Replied to user " + firstName);
        prepareAndSendMessage(chatId, answer);
    }

    private void exchangeRate (long chatId, String firstRate, String secondRate) {
        CurrencyConversion conversion = MonetaryConversions.getConversion(firstRate);

        MonetaryAmount sRate = Money.of(1, secondRate);

        MonetaryAmount inFirstConversion = sRate.with(conversion);
        String rate = "Rate of " + sRate + ":" + inFirstConversion;
        prepareAndSendMessage(chatId, rate);
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.info("Error occurred: " + e.getMessage());
        }
    }
}

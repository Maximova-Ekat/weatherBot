import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherBot extends TelegramLongPollingBot {

    private static final String BOT_TOKEN = "";
    private static final String BOT_USERNAME = "Weather_W1se_Bot";
    private static final String WEATHER_API_KEY = "c215c7d171aa0faf347de95d36f87980";
    private final Map<String, String> userStates = new HashMap<>();
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new WeatherBot());
            System.out.println("Бот запущен.");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                String userMessage = update.getMessage().getText();
                String chatId = update.getMessage().getChatId().toString();
                String userName = update.getMessage().getFrom().getUserName();
                System.out.println("Пользовательский запрос: [" + userName + "] " + userMessage);
                String response = processMessage(chatId, userMessage);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(response);
                ReplyKeyboardMarkup keyboardMarkup = KeyboardHandler.getKeyboard();
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

            } else {
                SendMessage message = new SendMessage();
                message.setChatId(update.getMessage().getChatId().toString());
                message.setText("Сообщение не распознано. Пожалуйста, отправьте текст.");

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String processMessage(String chatId, String userMessage) {
        if (userMessage.equals("/start")) {
            userStates.put(chatId, null);
            return "Привет! Я бот, который помогает узнать погоду. Напишите название города для прогноза.";
        }
        if (userMessage.equals("Прогноз на завтра")) {
            String currentCity = userStates.get(chatId);
            if (currentCity == null) {
                return "Сначала введите название города, чтобы получить прогноз на завтра.";
            }
            return CityWeatherHandler.getTomorrowWeather(currentCity, WEATHER_API_KEY);
        }
        else if (userMessage.equals("Скорость ветра")){
            String currentCity = userStates.get(chatId);
            if (currentCity == null) {
                return "Сначала введите название города, чтобы получить скорость ветра.";
            }
            return CityWeatherHandler.getWindInfo(currentCity, WEATHER_API_KEY);
        }
        userStates.put(chatId, userMessage);
        return CityWeatherHandler.getWeatherDescription(userMessage, WEATHER_API_KEY);
    }
    public String getBotUsername() {
        return BOT_USERNAME;
    }
    public String getBotToken() {
        return BOT_TOKEN;
    }
}

class KeyboardHandler {
    public static ReplyKeyboardMarkup getKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Москва"));
        row1.add(new KeyboardButton("Санкт-Петербург"));
        row1.add(new KeyboardButton("Екатеринбург"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Прогноз на завтра"));
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("Скорость ветра"));
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }
}

class CityWeatherHandler {
    public static String getWeatherDescription(String city, String apiKey) {
        String formattedCity = city.trim().replace(" ", "%20"); 
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + formattedCity +
                "&appid=" + apiKey + "&units=metric&lang=ru";
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            JSONObject jsonResponse = new JSONObject(response.toString());
            String cityName = jsonResponse.getString("name");
            JSONObject sysInfo = jsonResponse.getJSONObject("sys");
            String country = sysInfo.getString("country");
            String description = jsonResponse.getJSONArray("weather").getJSONObject(0).getString("description");
            double temperature = jsonResponse.getJSONObject("main").getDouble("temp");
            String clothingAdvice = getClothingAdvice(temperature, description);
            return "Город: " + cityName + " (" + country + ")\n" +
                    "Описание: " + description + "\n" +
                    "Температура: " + temperature + "°C\n" +
                    clothingAdvice;
        } catch (Exception e) {
            return "Не удалось получить данные для города: " + city + ". Проверьте правильность написания.";
        }
    }
    private static String getClothingAdvice ( double temperature, String description){
        StringBuilder advice = new StringBuilder("Советы по одежде:\n");
        if(temperature <= -20){
            advice.append("- Наденьте тёплую куртку, шапку и шарф.\n");
            advice.append("- Возьмите перчатки или варежки.\n");
            advice.append("- Лучше оставайтесь дома.\n");
        }
        else if (temperature <= -10) {
            advice.append("- Наденьте тёплую куртку, шапку и шарф.\n");
            advice.append("- Возьмите перчатки или варежки.\n");
        }else if (temperature<0) {
            advice.append("- Наденьте куртку потеплее и не забудьте шапку.\n");
        }else if (temperature <= 10) {
            advice.append("- Подойдёт осенняя куртка или пальто.\n");
            advice.append("- Возможно, вам понадобится лёгкая шапка.\n");
        } else if (temperature <= 20) {
            advice.append("- Лёгкая куртка или толстовка будут кстати.\n");
        } else {
            advice.append("- Наденьте футболку и лёгкие брюки или шорты.\n");
            advice.append("- Не забудьте головной убор, если солнечно.\n");
        }
        if (description.contains("дождь") || description.contains("ливень")) {
            advice.append("- Возьмите зонт или дождевик.\n");
            advice.append("- Наденьте непромокаемую обувь.\n");
        } else if (description.contains("снег")) {
            advice.append("- Наденьте обувь с нескользящей подошвой.\n");
        }
        return advice.toString();
    }

   private static String getSpecificTimeWeather(JSONArray forecastList, String periodName, int hourTarget, boolean isTomorrow) {
        for (int i = 0; i < forecastList.length(); i++) {
            JSONObject forecast = forecastList.getJSONObject(i);
            String dateText = forecast.getString("dt_txt");
            String[] dateParts = dateText.split(" ");
            int hour = Integer.parseInt(dateParts[1].split(":")[0]);
            boolean isToday = dateParts[0].equals(java.time.LocalDate.now().toString());
            boolean isForTomorrow = dateParts[0].equals(java.time.LocalDate.now().plusDays(1).toString());
            if ((isTomorrow && isForTomorrow || !isTomorrow && isToday) && hour == hourTarget) {
                String description = forecast.getJSONArray("weather").getJSONObject(0).getString("description");
                double temperature = forecast.getJSONObject("main").getDouble("temp");
                return periodName + ": " + description + ", " + temperature + "°C\n";
            }
        }
        return periodName + ": данные отсутствуют.\n";
    }

    public static String getTomorrowWeather(String city, String apiKey) {
        String formattedCity = city.trim().replace(" ", "%20");
        String apiUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + formattedCity +
                "&appid=" + apiKey + "&units=metric&lang=ru";
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray forecastList = jsonResponse.getJSONArray("list");
            StringBuilder result = new StringBuilder("Прогноз на завтра для города " + city + ":\n");
            result.append(getSpecificTimeWeather(forecastList, "Ночь", 3, true));
            result.append(getSpecificTimeWeather(forecastList, "Утро", 9, true));
            result.append(getSpecificTimeWeather(forecastList, "День", 15, true));
            result.append(getSpecificTimeWeather(forecastList, "Вечер", 21, true));
            return result.toString();
        } catch (Exception e) {
            return "Не удалось получить прогноз на завтра для города: " + city + ". Проверьте правильность написания.";
        }
    }
    public static String getWindInfo(String city, String apiKey) {
        String formattedCity = city.trim().replace(" ", "%20");
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + formattedCity +
                "&appid=" + apiKey + "&units=metric&lang=ru";
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            JSONObject jsonResponse = new JSONObject(response.toString());
            double windSpeed = jsonResponse.getJSONObject("wind").getDouble("speed");
            int windDegree = jsonResponse.getJSONObject("wind").getInt("deg");
            String windDirection = getWindDirection(windDegree);
            String windComment = getWindComment(windSpeed);
            return "Скорость ветра: " + windSpeed + " м/с\n" +
                    "Направление ветра: " + windDirection+ "\n" +
                    windComment;
        } catch (Exception e) {
            return "Не удалось получить данные о ветре для города " + city + ".";
        }
    }
    private static String getWindComment(double windSpeed) {
        if (windSpeed <= 1.7) {
            return "Идеально для прогулки — ветра почти нет!";
        } else if (windSpeed <= 3.3) {
            return "Легкий ветерок — наслаждайтесь свежим воздухом.";
        } else if (windSpeed <= 7.4) {
            return "Держите шляпу, чтобы не улетела!";
        } else if (windSpeed <= 12.4) {
            return "Ветерок уже чувствуется — крепче держитесь за поручни!";
        } else {
            return "Аккуратно, вас может сдуть!";
        }
    }
    private static String getWindDirection(int degree) {
        String[] directions = {"Север", "Северо-восток", "Восток", "Юго-восток",
                "Юг", "Юго-запад", "Запад", "Северо-запад"};
        return directions[(int)Math.round((degree % 360) / 45.0) % 8];
    }
}


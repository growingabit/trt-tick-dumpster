package it.growbit.servlet;

import com.google.common.collect.Lists;
import it.growbit.matlab.Matlab;
import it.growbit.matlab.model.Last24HoursAvg;
import it.growbit.matlab.model.Next24HourAvg;
import it.growbit.model.trt.Trades_last_24;
import it.growbit.telegram.Telegram;
import it.growbit.telegram.model.SendMessage;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by name on 23/06/17.
 */
public class TaskDailyHourAvg extends HttpServlet {

    private static final Logger log = Logger.getLogger(TaskDailyHourAvg.class.getName());

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        List<Trades_last_24> l24avgs = Trades_last_24.list();

        // reverse perche' la vista sql le ritorna DESC
        Last24HoursAvg matlab_model = new Last24HoursAvg(Lists.reverse(l24avgs));

        Next24HourAvg forecast;
        try {
            forecast = Matlab.criptoOracleValori(matlab_model);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        Double forecast_trend = forecast.getAvg();
        Double last24avg = doAvg(l24avgs);
        Double forecast_price = ((forecast_trend / 100) * last24avg) + last24avg;

        String telegram_message = "";
        telegram_message += "In base all'ultimo valore di " + l24avgs.get(0).getTf_price() + ", con media 24h di " + last24avg + " visto alle " + l24avgs.get(0).getTf_hour() + " UTC";
        telegram_message += ", criptoOracleValori dice: " + forecast_trend.toString();
        telegram_message += ". Il prezzo medio delle prossime 24h sara' " + forecast_price;

        Telegram.sendMessage(new SendMessage(Telegram.props.getProperty(Telegram.PROPERTY_SCALP_CAVERNA), telegram_message));

    }

    private Double doAvg(List<Trades_last_24> l24avgs) {
        Double totale = 0d;

        for (Trades_last_24 t : l24avgs) {
            totale += t.getTf_price();
        }

        return totale / l24avgs.size();
    }
}

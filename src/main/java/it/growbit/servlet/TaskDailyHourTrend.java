package it.growbit.servlet;

import com.google.common.collect.Lists;
import it.growbit.flex.GAEFlexAutoScaler;
import it.growbit.matlab.Matlab;
import it.growbit.matlab.model.Last24HoursAvg;
import it.growbit.matlab.model.Last24HoursTrend;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Created by name on 23/06/17.
 */
public class TaskDailyHourTrend extends HttpServlet {

    private static final Logger log = Logger.getLogger(TaskDailyHourTrend.class.getName());

    private static final GAEFlexAutoScaler fas = GAEFlexAutoScaler.singleton("matlab", "20170709t143018");

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Future<Boolean> async_matlab_start = fas.start();

        List<Trades_last_24> l24avgs = Trades_last_24.list(25);

        // reverse perche' la vista sql le ritorna DESC
        Last24HoursAvg matlab_model = new Last24HoursTrend(Lists.reverse(l24avgs));

        Next24HourAvg forecast;
        try {
            async_matlab_start.get();
            forecast = Matlab.superCriptoOracleTrend(matlab_model);
            fas.stop_as_task();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return;
        }

        String telegram_message = "";
        telegram_message += "In base all'ultimo valore di " + l24avgs.get(0).getTf_price() + ", visto alle " + l24avgs.get(0).getTf_hour() + " UTC";
        telegram_message += ", superCriptoOracleTrend dice(+1 sale, -1 scende) riguardo alla prossima ora: " + forecast.getAvg().toString();
        log.info(telegram_message);
//        Telegram.sendMessage(new SendMessage(Telegram.props.getProperty(Telegram.PROPERTY_SCALP_CAVERNA), telegram_message));

    }
}

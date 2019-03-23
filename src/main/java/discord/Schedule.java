package discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import rx.subjects.PublishSubject;

public class Schedule
{
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(AppController.class);
    private Timer timer;
    private PublishSubject<String> subject;

    public Schedule()
    {
        subject = PublishSubject.create();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                reportCurrentTime();
            }
        },0,5000);
    }

    public PublishSubject<String> GetSubject()
    {
        return subject;
    }

    public void reportCurrentTime()
    {
        String str = dateFormat.format(new Date());
        subject.onNext(str);
    }
}
package discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Schedule
{
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(AppController.class);
    
    private Timer timer;
    private List<SseEmitter> emitters;
    private ExecutorService nonBlockingService = Executors
    .newCachedThreadPool();

    public Schedule()
    {
        timer = new Timer();
        emitters = new ArrayList<SseEmitter>();
        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                reportCurrentTime();
            }
        },0,5000);
    }

    public void reportCurrentTime()
    {
        String str = dateFormat.format(new Date());

        for (SseEmitter emitter : emitters) {
            nonBlockingService.execute(() ->{
                try
                {
                    log.info("emit");
                    emitter.send(str);
                }
                catch(Exception ex)
                {
                    log.info("ex");
                    removeEmitter(emitter);
                }
            });
        }
    }

    public void addEmitter(SseEmitter emitter)
    {
        emitters.add(emitter);
    }

    public void removeEmitter(SseEmitter emitter)
    {
        if(emitters.contains(emitter))
        emitters.remove(emitter);
    }
}
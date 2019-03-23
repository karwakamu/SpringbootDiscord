package discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
public class AppController
{

    private static final Logger log = LoggerFactory.getLogger(AppController.class);
    private static SseEmitter emitter;
    private Schedule schedule;

    public AppController()
    {
        schedule = new Schedule();
    }

    public void initEmitter ()
    {
        emitter = new SseEmitter();
        emitter.onTimeout(new Runnable(){
        
            @Override
            public void run() {
                log.info("timeout");
                initEmitter();
            }
        });
        schedule.addEmitter(emitter);
    }

    @GetMapping("/default")
    public String defaultGet() {
        initEmitter();
        return "default";
    }

    @GetMapping("/sse")
    public SseEmitter handleSse()
    {
        return emitter;
    }
}
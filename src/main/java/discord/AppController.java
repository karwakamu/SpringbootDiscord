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
    private Schedule schedule;

    public AppController()
    {
        schedule = new Schedule();
    }

    @GetMapping("/default")
    public String defaultGet()
    {
        return "default";
    }

    private void notifyProgress(SseEmitter emitter, String str)
    {
        try
        {
            emitter.send(str);
        }
        catch (Exception ex) {
            log.info(ex.getMessage());
            emitter.completeWithError(ex);
        }
    }

    @GetMapping("/sse")
    public SseEmitter streamSseMvc() {
        SseEmitter emitter = new SseEmitter(1440000L);
        schedule.GetSubject().subscribe(value -> notifyProgress(emitter, value),
        emitter::completeWithError,
        emitter::complete);
        return emitter;
    }
}
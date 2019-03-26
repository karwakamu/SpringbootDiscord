package discord;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rx.Subscription;

public class SubscribedEmitter extends SseEmitter
    {
        private Subscription subscription;

        public SubscribedEmitter(long timeout)
        {
            super(timeout);
        }

        public void SetSubscription(Subscription subscription)
        {
            this.subscription = subscription;
        }

        public void Unsubscribe()
        {
            subscription.unsubscribe();
        }
    }
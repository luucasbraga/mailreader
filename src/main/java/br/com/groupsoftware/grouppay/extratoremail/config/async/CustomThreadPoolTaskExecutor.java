package br.com.groupsoftware.grouppay.extratoremail.config.async;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class CustomThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {


    @Override
    public void execute(Runnable task) {
        final Authentication a = SecurityContextHolder.getContext().getAuthentication();

        super.execute(new Runnable() {
            public void run() {
                try {
                    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                    ctx.setAuthentication(a);
                    SecurityContextHolder.setContext(ctx);
                    task.run();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }
        });
    }
}

package club.ihere.core.util.commit;

import club.ihere.core.config.BaseConfig;
import club.ihere.core.config.Config;
import club.ihere.core.constant.Constant;
import club.ihere.core.exception.ParameterValidationException;
import club.ihere.core.util.StringUtil;
import club.ihere.core.util.commit.annotation.AvoidRepeatableCommit;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author: fengshibo
 * @date: 2018/11/1 11:31
 * @description:
 */
public class AvoidRepeatableCommitAdvice implements MethodInterceptor {

    @Autowired
    @Qualifier("redisTemplate6")
    private RedisTemplate redisTemplate;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ip = getIp2(request);
        Method method = methodInvocation.getMethod();
        //目标类、方法
        String className = method.getDeclaringClass().getName();
        String name = method.getName();
        String ipKey = String.format("%s#%s", className, name);
        int hashCode = Math.abs(ipKey.hashCode());
        String key = String.format("%s_%d", ip, hashCode);
        AvoidRepeatableCommit avoidRepeatableCommit = method.getAnnotation(AvoidRepeatableCommit.class);
        long timeout = avoidRepeatableCommit.timeout();
        if (timeout < 0) {
			//常量，如果注解时间不合法则赋予默认值
            timeout = Constant.AVOID_REPEATABLE_COMMIT_TIME;
        }
        String value = (String) redisTemplate.opsForValue().get(key);
        if (StringUtil.isNotBlank(value)) {
            throw new ParameterValidationException("请勿重复提交");
        }
        redisTemplate.opsForValue().set(key, UUID.randomUUID().toString().replace("-", ""), timeout, TimeUnit.MILLISECONDS);
        return methodInvocation.proceed();
    }

    private String getIp2(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtil.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
            //多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = ip.indexOf(",");
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtil.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}

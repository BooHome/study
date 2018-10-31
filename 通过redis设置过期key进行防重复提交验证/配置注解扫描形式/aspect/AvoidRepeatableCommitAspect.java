package club.ihere.aspect;

/**
 * @author: fengshibo
 * @date: 2018/10/31 09:42
 * @description:
 */

import club.ihere.core.exception.ParameterValidationException;
import club.ihere.core.util.StringUtil;
import club.ihere.aspect.annotation.AvoidRepeatableCommit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import sun.net.util.IPAddressUtil;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 重复提交aop
 *
 * @author
 * @since
 */
@Aspect
@Component
public class AvoidRepeatableCommitAspect {


    @Autowired
    @Qualifier("redisTemplate6")
    private RedisTemplate redisTemplate;

    /**
     * @param point
     */
    @Around("@annotation(club.ihere.social.qa.aspect.annotation.AvoidRepeatableCommit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ip = getIp2(request);
        //获取注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        //目标类、方法
        String className = method.getDeclaringClass().getName();
        String name = method.getName();
        String ipKey = String.format("%s#%s", className, name);
        int hashCode = Math.abs(ipKey.hashCode());
        String key = String.format("%s_%d", ip, hashCode);
        AvoidRepeatableCommit avoidRepeatableCommit = method.getAnnotation(AvoidRepeatableCommit.class);
        long timeout = avoidRepeatableCommit.timeout();
        if (timeout < 0) {
            timeout = 3000L;
        }
        String value = (String) redisTemplate.opsForValue().get(key);
        if (StringUtil.isNotBlank(value)) {
            throw new ParameterValidationException("请勿重复提交");
        }
        redisTemplate.opsForValue().set(key, UUID.randomUUID().toString().replace("-", ""), timeout, TimeUnit.MILLISECONDS);
        //执行方法
        Object object = point.proceed();
        return object;
    }

    private  String getIp2(HttpServletRequest request) {
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

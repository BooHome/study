package club.ihere.core.util.commit.annotation;

/**
 * @author: fengshibo
 * @date: 2018/10/31 09:39
 * @description:
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 避免重复提交
 * @author hhz
 * @version
 * @since
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AvoidRepeatableCommit {

    /**
     * 指定时间内不可重复提交,单位毫秒
     * @return
     */
    long timeout()  default 3000 ;

}

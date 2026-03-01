
package com.yupi.yupicturebackend.aop;

import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.UserRoleEnum;
import com.yupi.yupicturebackend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {

        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        log.info("权限校验 - 用户角色: {}, 需要角色: {}", loginUser.getUserRole(), mustRole);
        
        // 使用智能查找方法
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumSmart(mustRole);
        if (mustRoleEnum == null) {
            log.info("无需特定权限，放行");
            return joinPoint.proceed();
        }
        
        // 获取用户角色
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumSmart(loginUser.getUserRole());
        if (userRoleEnum == null) {
            log.warn("用户角色无效: {}", loginUser.getUserRole());
            throw new BusinessException(ErrorCode.NOT_AUTHORIZED_ERROR, "用户角色无效");
        }
        
        log.info("权限检查 - 用户角色: {}, 需要角色: {}", userRoleEnum, mustRoleEnum);
        
        // 权限检查
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            log.warn("权限不足 - 用户角色: {}, 需要角色: {}", userRoleEnum.getValue(), mustRoleEnum.getValue());
            throw new BusinessException(ErrorCode.NOT_AUTHORIZED_ERROR, "权限不足");
        }
        
        log.info("权限校验通过");
        return joinPoint.proceed();
    }
}
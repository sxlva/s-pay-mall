package cn.fcr.domain.mall.model.entity;

import cn.fcr.types.common.Constants;
import org.junit.Test;

import static org.junit.Assert.*;

public class UserEntityTest {

    @Test
    public void testValidateLoginStatus_ActiveUser_ShouldPass() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .status(Constants.USER_STATUS_ACTIVE)
                .build();

        assertDoesNotThrow(user::validateLoginStatus);
    }

    @Test
    public void testValidateLoginStatus_DisabledUser_ShouldThrowException() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("disabledUser")
                .password("encodedPassword")
                .status(Constants.USER_STATUS_DISABLED)
                .build();

        IllegalStateException exception = assertThrows(IllegalStateException.class, user::validateLoginStatus);
        assertTrue(exception.getMessage().contains("账号已被封禁"));
    }

    @Test
    public void testValidateLoginStatus_NullStatus_ShouldPass() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .status(null)
                .build();

        assertDoesNotThrow(user::validateLoginStatus);
    }

    @Test
    public void testValidatePassword_MatchingPassword_ShouldReturnTrue() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword123")
                .status(Constants.USER_STATUS_ACTIVE)
                .build();

        boolean result = user.validatePassword("rawPassword", 
                (raw, encoded) -> encoded.equals("encodedPassword123"));
        assertTrue(result);
    }

    @Test
    public void testValidatePassword_NonMatchingPassword_ShouldReturnFalse() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword123")
                .status(Constants.USER_STATUS_ACTIVE)
                .build();

        boolean result = user.validatePassword("wrongPassword", 
                (raw, encoded) -> encoded.equals("wrongEncodedPassword"));
        assertFalse(result);
    }

    @Test
    public void testValidatePassword_NullEncodedPassword_ShouldReturnFalse() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .password(null)
                .status(Constants.USER_STATUS_ACTIVE)
                .build();

        boolean result = user.validatePassword("anyPassword", 
                (raw, encoded) -> encoded != null && encoded.equals("encoded"));
        assertFalse(result);
    }

    @Test
    public void testGetRoleOrDefault_HasRoleCode_ShouldReturnRoleCode() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("admin")
                .roleCode("ADMIN")
                .build();

        assertEquals("ADMIN", user.getRoleOrDefault());
    }

    @Test
    public void testGetRoleOrDefault_NullRoleCode_ShouldReturnDefaultRole() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("user")
                .roleCode(null)
                .build();

        assertEquals(Constants.DEFAULT_ROLE_MEMBER, user.getRoleOrDefault());
    }

    @Test
    public void testGetRoleOrDefault_EmptyRoleCode_ShouldReturnDefaultRole() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("user")
                .roleCode("")
                .build();

        assertEquals(Constants.DEFAULT_ROLE_MEMBER, user.getRoleOrDefault());
    }

    @Test
    public void testGetRoleOrDefault_BlankRoleCode_ShouldReturnDefaultRole() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("user")
                .roleCode("   ")
                .build();

        assertEquals(Constants.DEFAULT_ROLE_MEMBER, user.getRoleOrDefault());
    }

    private void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            fail("Expected no exception but got: " + e.getMessage());
        }
    }

    private IllegalStateException assertThrows(Class<IllegalStateException> exceptionClass, Runnable runnable) {
        try {
            runnable.run();
            fail("Expected exception of type " + exceptionClass.getName() + " but none was thrown");
            return null;
        } catch (IllegalStateException e) {
            return e;
        }
    }
}
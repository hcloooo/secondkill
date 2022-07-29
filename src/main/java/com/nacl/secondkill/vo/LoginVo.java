package com.nacl.secondkill.vo;

import com.nacl.secondkill.validator.IsMobile;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

@Data
public class LoginVo {

    @NotNull
    @IsMobile
    private String mobile;

    @Length(min=32)
    private String password;
}

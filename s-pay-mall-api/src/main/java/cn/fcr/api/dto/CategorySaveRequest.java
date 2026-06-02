package cn.fcr.api.dto;

import lombok.Data;

@Data
public class CategorySaveRequest {

    private Long id;

    private String name;

    private Integer status;
}
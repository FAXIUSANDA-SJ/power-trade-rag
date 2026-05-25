package com.powertrade.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powertrade.dal.entity.IngestTaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IngestTaskMapper extends BaseMapper<IngestTaskEntity> {
}

package com.hollysys.ppa.module.curve.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hollysys.ppa.module.curve.entity.EquipmentCurve;
import com.hollysys.ppa.module.curve.entity.HealthScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工况曲线 Mapper
 *
 * @author PPA Team
 */
@Mapper
public interface EquipmentCurveMapper extends BaseMapper<EquipmentCurve> {

    @Select("SELECT * FROM t_equipment_curve " +
            "WHERE deleted = 0 AND unit_id = #{unitId} AND device_code = #{deviceCode} " +
            "AND curve_type = #{curveType} " +
            "AND sample_time BETWEEN #{start} AND #{end} " +
            "ORDER BY sample_time ASC")
    List<EquipmentCurve> selectCurve(@Param("unitId") Long unitId,
                                      @Param("deviceCode") String deviceCode,
                                      @Param("curveType") String curveType,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Select("SELECT * FROM t_health_score " +
            "WHERE deleted = 0 AND unit_id = #{unitId} AND device_code = #{deviceCode} " +
            "ORDER BY score_time DESC LIMIT 1")
    HealthScore selectLatestHealth(@Param("unitId") Long unitId,
                                    @Param("deviceCode") String deviceCode);
}

package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Resource;

import java.util.List;
import java.util.Optional;

public interface ResourceDao {

    int insert(Resource resource);

    Optional<Resource> findByCode(String code);

    List<IdName> findCategories();

    ReportTable listReport(String statusFilter);

    void updateStatus(int resourceId, String status);
}

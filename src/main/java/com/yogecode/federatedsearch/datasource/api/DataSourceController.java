package com.yogecode.federatedsearch.datasource.api;

import com.yogecode.federatedsearch.api.datasource.ConnectionTestResponse;
import com.yogecode.federatedsearch.api.datasource.CreateDataSourceRequest;
import com.yogecode.federatedsearch.api.datasource.DataSourceDetailsResponse;
import com.yogecode.federatedsearch.api.datasource.DataSourceResponse;
import com.yogecode.federatedsearch.datasource.service.DataSourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/datasources")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GetMapping
    public List<DataSourceDetailsResponse> list() {
        return dataSourceService.getAllDetails();
    }

    @GetMapping("/{id}")
    public DataSourceDetailsResponse get(@PathVariable("id") Long id) {
        return dataSourceService.getDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Datasource not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataSourceResponse create(@Valid @RequestBody CreateDataSourceRequest request) {
        return dataSourceService.create(request);
    }

    @PostMapping("/{id}/test")
    public ConnectionTestResponse testConnection(@PathVariable("id") Long id) {
        return dataSourceService.testConnection(id);
    }
}

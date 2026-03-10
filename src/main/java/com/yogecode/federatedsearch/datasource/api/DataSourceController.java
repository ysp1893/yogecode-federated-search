package com.yogecode.federatedsearch.datasource.api;

import com.yogecode.federatedsearch.api.datasource.ConnectionTestResponse;
import com.yogecode.federatedsearch.api.datasource.CreateDataSourceRequest;
import com.yogecode.federatedsearch.api.datasource.DataSourceDetailsResponse;
import com.yogecode.federatedsearch.api.datasource.DataSourceResponse;
import com.yogecode.federatedsearch.datasource.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Datasource Admin", description = "Register, inspect, and validate external datasource configurations.")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GetMapping
    @Operation(summary = "List datasources", description = "Returns all registered datasource configurations without exposing credential references.")
    public List<DataSourceDetailsResponse> list() {
        return dataSourceService.getAllDetails();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get datasource", description = "Returns a single registered datasource by id.")
    @ApiResponse(responseCode = "404", description = "Datasource not found")
    public DataSourceDetailsResponse get(@PathVariable("id") Long id) {
        return dataSourceService.getDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Datasource not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create datasource", description = "Registers a datasource that can later be bound to entities and searched through metadata.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Datasource registration payload",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "MySQL datasource",
                            value = """
                                    {
                                      "sourceCode": "cust_mysql",
                                      "sourceName": "Customer MySQL",
                                      "dbType": "MYSQL",
                                      "host": "localhost",
                                      "port": 3306,
                                      "databaseName": "customer_db",
                                      "username": "app_user",
                                      "password": "secret",
                                      "connectionParams": {
                                        "ssl": false
                                      }
                                    }
                                    """
                    )
            )
    )
    public DataSourceResponse create(@Valid @org.springframework.web.bind.annotation.RequestBody CreateDataSourceRequest request) {
        return dataSourceService.create(request);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Test datasource", description = "Checks whether a registered datasource entry exists and is available for connector-level validation.")
    public ConnectionTestResponse testConnection(@PathVariable("id") Long id) {
        return dataSourceService.testConnection(id);
    }
}

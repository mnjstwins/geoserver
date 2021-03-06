/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.nsg.pagination.random;

import net.opengis.wfs20.GetFeatureType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This dispatcher manages service of type {@link PageResultsWebFeatureService} and sets the
 * parameter ResultSetID present on KVP map.
 * <p>
 * Dummy featureId value is added to KVP map to allow dispatcher to manage it as usual WFS 2.0
 * request.
 *
 * @author sandr
 */

public class PageResultsDispatcherCallback extends AbstractDispatcherCallback {

    static final String PAGE_RESULTS = "PageResults";

    private final static Logger LOGGER = Logging.getLogger(PageResultsDispatcherCallback.class);
    private final PageResultsWebFeatureService service;
    private GeoServer gs;


    public PageResultsDispatcherCallback(GeoServer gs, PageResultsWebFeatureService service) {
        this.gs = gs;
        this.service = service;

        // configure the extra operation in WFS 2.0
        List<Service> services = GeoServerExtensions.extensions(Service.class);
        for (Service s : services) {
            if ("wfs".equals(s.getId().toLowerCase()) && Integer.valueOf(2).equals(s.getVersion().getMajor())) {
                if (!s.getOperations().contains(PAGE_RESULTS)) {
                    s.getOperations().add(PAGE_RESULTS);
                }
            }
        }
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        Object req = request.getKvp().get("REQUEST");
        if ("wfs".equals(service.getId().toLowerCase())
                && PAGE_RESULTS.equals(req)) {
            // allow the request to be successfully parsed as a GetFeature (needs at least a typename or a featureId)
            request.getKvp().put("featureId", Collections.singletonList("dummy"));
            // replace the service
            return new Service(service.getId(), this.service, service.getVersion(),
                    service.getOperations());
        }
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        Operation newOperation = operation;
        // Change operation from PageResults to GetFeature to allow management of request as
        // standard get feature
        if (operation.getId().equals("PageResults")) {
            newOperation = new Operation("GetFeature", operation.getService(),
                    operation.getMethod(), operation.getParameters());
        }
        return super.operationDispatched(request, newOperation);
    }

}

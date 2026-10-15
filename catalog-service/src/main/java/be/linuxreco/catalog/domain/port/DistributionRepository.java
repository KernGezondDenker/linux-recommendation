package be.linuxreco.catalog.domain.port;

import be.linuxreco.catalog.domain.model.Distribution;
import java.util.List;


public interface DistributionRepository {

    List<Distribution> findAll();
}

package dragger.bl.exporter;

import dragger.bl.executor.QueryExecutor;
import dragger.bl.generator.QueryGenerator;
import dragger.entities.ChartQueryFilter;
import dragger.entities.ReportQueryFilter;
import dragger.entities.charts.Chart;
import dragger.entities.charts.ChartColumnResult;
import dragger.entities.charts.ChartResult;
import dragger.exceptions.DraggerException;
import dragger.exceptions.DraggerExportException;
import dragger.repositories.FilterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@Named
public class ChartRestResultsExporter implements ChartQueryExporter {
	private static final int COUNT_COLUMN_INDEX = 1;
	private static final int DATA_COLUMN_INDEX = 2;
	private static final String EMPTY = "";
	private static final boolean SHOW_DUPLICATES = true;

	@Inject
	private QueryGenerator generator;
	@Inject
	private QueryExecutor executor;
	@Inject
	private ChartExecutionResultExporter executionResultExporter;
	@Inject
	private FilterRepository filterRepository;


	public Collection<ChartColumnResult> export(Chart chartQuery, Collection<ChartQueryFilter> filters)
			throws DraggerExportException {
		SqlRowSet results;

		log.info("executing chart query (id = " + chartQuery.getId() + ")");
		try {
			results = executor.executeQuery(
					generator.generate(chartQuery.getQuery(), convertToQueryFilters(filters), SHOW_DUPLICATES));
		} catch (DraggerException e) {
			log.error("execution of chart query (id = " + chartQuery.getId() + ")" + " failed");
			throw new DraggerExportException("Could not generate the chart query (id = " + chartQuery.getId() + ")", e);
		}

		Collection<ChartColumnResult> chartResults = new ArrayList<>();
		while (results.next()) {

			long count = results.getLong(COUNT_COLUMN_INDEX);
			Object label = results.getObject(DATA_COLUMN_INDEX);

			if (label == null) {
				label = EMPTY;
			}

			chartResults.add(new ChartColumnResult(label.toString(), count));
		}

		log.info("chart query (id = " + chartQuery.getId() + ")" + " executed successfully");
		executionResultExporter.export(new ChartResult(chartResults), chartQuery);

		return chartResults;
	}

	private Collection<ReportQueryFilter> convertToQueryFilters(Collection<ChartQueryFilter> filters) {
		return filters.stream().map(chartFilter -> convertToQueryFilter(chartFilter)).collect(Collectors.toList());
	}

	private ReportQueryFilter convertToQueryFilter(ChartQueryFilter filter) {
		return new ReportQueryFilter(filterRepository.findById(filter.getFilterId()).get(), filter.getColumn(),
				filter.getValue());
	}
}
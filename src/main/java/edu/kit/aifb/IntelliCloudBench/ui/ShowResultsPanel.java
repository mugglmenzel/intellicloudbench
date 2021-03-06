/*
 * This file is part of IntelliCloudBench.
 *
 * Copyright (c) 2012, Jan Gerlinger <jan.gerlinger@gmx.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the Institute of Applied Informatics and Formal
 * Description Methods (AIFB) nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.kit.aifb.IntelliCloudBench.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.vaadin.data.Property.ConversionException;
import com.vaadin.data.Property.ReadOnlyException;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.terminal.SystemError;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import edu.kit.aifb.IntelliCloudBench.model.UIState;
import edu.kit.aifb.libIntelliCloudBench.CloudBenchService;
import edu.kit.aifb.libIntelliCloudBench.metrics.IInstanceOrderer;
import edu.kit.aifb.libIntelliCloudBench.metrics.IMetricsResult;
import edu.kit.aifb.libIntelliCloudBench.metrics.IMetricsResult.Proportion;
import edu.kit.aifb.libIntelliCloudBench.metrics.IMetricsType;
import edu.kit.aifb.libIntelliCloudBench.metrics.MetricsConfiguration;
import edu.kit.aifb.libIntelliCloudBench.metrics.RelativeToReferenceInstanceOrderer;
import edu.kit.aifb.libIntelliCloudBench.model.Benchmark;
import edu.kit.aifb.libIntelliCloudBench.model.InstanceType;
import edu.kit.aifb.libIntelliCloudBench.model.json.CostsStore;
import edu.kit.aifb.libIntelliCloudBench.model.xml.Result;
import edu.kit.aifb.libIntelliCloudBench.util.MetricsHelper;

public class ShowResultsPanel extends Panel {
	private static final long serialVersionUID = -1107106613769316398L;

	private VerticalLayout content;

	private VerticalLayout metricsResultLayout;

	private CloudBenchService service;

	private VerticalLayout weightResultsLayout;

	private boolean costsChecked = true;

	private VerticalLayout weightedResultsLayout = new VerticalLayout();

	private UIState uiState;

	public ShowResultsPanel(String caption, CloudBenchService service, UIState uiState) {
		super();

		setCaption(caption);
		content = ((VerticalLayout) getContent());
		content.setSpacing(true);

		this.uiState = uiState;
		this.service = service;

		Map<InstanceType, Multimap<Benchmark, Result>> resultsForAllBenchmarksForType =
		    service.getResultsForAllBenchmarksForType();

		for (InstanceType instanceType : resultsForAllBenchmarksForType.keySet()) {
			HorizontalLayout benchmarkLayout = new HorizontalLayout();

			benchmarkLayout.setStyleName("grey_background");
			benchmarkLayout.setMargin(true);
			benchmarkLayout.setSpacing(true);
			benchmarkLayout.setWidth("100%");

			VerticalLayout instanceInfo = new VerticalLayout();
			instanceInfo.setMargin(true);
			instanceInfo.setSpacing(true);
			instanceInfo.setWidth("100%");

			Label providerLabel = new Label(instanceType.getProvider().getName());
			instanceInfo.addComponent(providerLabel);

			Label regionLabel = new Label(instanceType.getRegion().getId());
			instanceInfo.addComponent(regionLabel);

			Label hardwareTypeLabel = new Label(instanceType.getHardwareType().getName());
			instanceInfo.addComponent(hardwareTypeLabel);

			benchmarkLayout.addComponent(instanceInfo);
			benchmarkLayout.setExpandRatio(instanceInfo, 1f);

			VerticalLayout benchmarkInfo = new VerticalLayout();
			benchmarkInfo.setMargin(true);
			benchmarkInfo.setSpacing(true);
			benchmarkInfo.setWidth("100%");

			for (Benchmark benchmark : resultsForAllBenchmarksForType.get(instanceType).keySet()) {
				Label benchmarkName = new Label(benchmark.getName());
				benchmarkInfo.addComponent(benchmarkName);

				Collection<Result> results = resultsForAllBenchmarksForType.get(instanceType).get(benchmark);
				if (!results.isEmpty()) {
					for (Result result : results) {
						Label benchmarkScore = new Label(result.getValueAsString() + " " + result.getScale());
						benchmarkInfo.addComponent(benchmarkScore);
					}
				} else {
					Label benchmarkError = new Label("Failed to run.");
					benchmarkInfo.addComponent(benchmarkError);
				}

			}

			benchmarkLayout.addComponent(benchmarkInfo);
			benchmarkLayout.setExpandRatio(benchmarkInfo, 4f);
			content.addComponent(benchmarkLayout);
		}

		if (service.getResultsForAllBenchmarksForType().keySet().size() > 1) {
			final VerticalLayout metricsLayout = new VerticalLayout();
			metricsLayout.setWidth("100%");
			metricsLayout.setSpacing(true);
			metricsLayout.setMargin(true);
			metricsLayout.setStyleName("grey_background");

			HorizontalLayout metricsSelectionLayout = new HorizontalLayout();
			metricsSelectionLayout.setWidth("100%");
			metricsSelectionLayout.setSpacing(true);
			metricsSelectionLayout.setMargin(true);

			final OptionGroup metricsSelector = new OptionGroup("Please select how benchmark results should be compared:");
			for (Class<? extends IInstanceOrderer> instanceOrderer : MetricsConfiguration.INSTANCE_ORDERER) {
				int index = MetricsConfiguration.INSTANCE_ORDERER.indexOf(instanceOrderer);
				metricsSelector.addItem(instanceOrderer);
				metricsSelector.setItemCaption(instanceOrderer, MetricsConfiguration.INSTANCE_ORDERER_NAMES.get(index));
			}
			metricsSelector.setNullSelectionAllowed(false);
			metricsSelector.select(uiState.getMetricsConfiguration().getSelectedInstanceOrderer());
			metricsSelector.setImmediate(true);
			metricsSelector.addListener(new ValueChangeListener() {
				private static final long serialVersionUID = -6167753078722196865L;

				@Override
				public void valueChange(ValueChangeEvent event) {
					VerticalLayout newMetricsResultLayout =
					    buildMetricsResultLayout((Class<IInstanceOrderer>) metricsSelector.getValue(), costsChecked);
					metricsLayout.replaceComponent(metricsResultLayout, newMetricsResultLayout);
					metricsResultLayout = newMetricsResultLayout;
				}

			});
			metricsSelectionLayout.addComponent(metricsSelector);

			final CheckBox costsCheckbox = new CheckBox("Include monthly costs", costsChecked);
			costsCheckbox.setImmediate(true);
			costsCheckbox.addListener(new ValueChangeListener() {
				private static final long serialVersionUID = -2462873118115972047L;

				@Override
				public void valueChange(ValueChangeEvent event) {
					costsChecked = (Boolean) costsCheckbox.getValue();
					VerticalLayout newMetricsResultLayout =
					    buildMetricsResultLayout((Class<IInstanceOrderer>) metricsSelector.getValue(), costsChecked);
					metricsLayout.replaceComponent(metricsResultLayout, newMetricsResultLayout);
					metricsResultLayout = newMetricsResultLayout;
				}

			});
			metricsSelectionLayout.addComponent(costsCheckbox);

			metricsLayout.addComponent(metricsSelectionLayout);

			metricsResultLayout =
			    buildMetricsResultLayout((Class<IInstanceOrderer>) metricsSelector.getValue(), costsChecked);
			metricsLayout.addComponent(metricsResultLayout);

			TextField stopperLogField = new TextField("Log for stopping method:");
			stopperLogField.setWidth("100%");
			stopperLogField.setHeight("250px");
			stopperLogField.setValue(service.getStopperLog());
			content.addComponent(stopperLogField);

			content.addComponent(metricsLayout);
		}
	}

	VerticalLayout buildMetricsResultLayout(Class<? extends IInstanceOrderer> instanceOrderer, final boolean costsChecked) {
		final VerticalLayout metricsResultLayout = new VerticalLayout();
		metricsResultLayout.setSpacing(true);
		metricsResultLayout.setMargin(true);

		if (uiState.getMetricsConfiguration().requiresReference()) {

			/* Select a reference of the instances */
			List<InstanceType> instanceTypes =
			    new ArrayList<InstanceType>(service.getResultsForAllBenchmarksForType().keySet());

			final NativeSelect referenceSelector = buildReferenceSelector(costsChecked, metricsResultLayout, instanceTypes);
			metricsResultLayout.addComponent(referenceSelector);

			weightResultsLayout =
			    buildWeightResultsLayout(metricsResultLayout, instanceOrderer, costsChecked, instanceTypes.get(0));

		} else {

			weightResultsLayout = buildWeightResultsLayout(metricsResultLayout, instanceOrderer, costsChecked);

		}
		metricsResultLayout.addComponent(weightResultsLayout);

		weightedResultsLayout = buildWeightedResultsLayout();
		metricsResultLayout.addComponent(weightedResultsLayout);

		TextField csvTextField = buildCsvTextField(instanceOrderer);
		csvTextField.setCaption("CSV-style results:");
		csvTextField.setWidth("100%");
		csvTextField.setHeight("250px");
		metricsResultLayout.addComponent(csvTextField);

		return metricsResultLayout;
	}

	private NativeSelect buildReferenceSelector(final boolean costsChecked, final VerticalLayout metricsResultLayout,
	    List<InstanceType> instanceTypes) throws UnsupportedOperationException, ReadOnlyException, ConversionException {
		final NativeSelect referenceSelector = new NativeSelect("Please select the reference instance:");
		for (InstanceType instanceType : instanceTypes) {
			referenceSelector.addItem(instanceType);
			referenceSelector.setItemCaption(instanceType, instanceType.asString(" | "));
		}

		referenceSelector.setNullSelectionAllowed(false);
		referenceSelector.setValue(uiState.getMetricsConfiguration().getSelectedReference());
		referenceSelector.setImmediate(true);
		referenceSelector.addListener(new ValueChangeListener() {
			private static final long serialVersionUID = -2462873118115972047L;

			@Override
			public void valueChange(ValueChangeEvent event) {
				VerticalLayout oldResultLabelsLayout = weightResultsLayout;
				weightResultsLayout =
				    buildWeightResultsLayout(
				        metricsResultLayout,
				        RelativeToReferenceInstanceOrderer.class,
				        costsChecked,
				        (InstanceType) referenceSelector.getValue());
				metricsResultLayout.replaceComponent(oldResultLabelsLayout, weightResultsLayout);

				VerticalLayout oldWeightedResultsLayout = weightedResultsLayout;
				weightedResultsLayout = buildWeightedResultsLayout();
				metricsResultLayout.replaceComponent(oldWeightedResultsLayout, weightedResultsLayout);
			}

		});
		return referenceSelector;
	}

	private VerticalLayout buildWeightedResultsLayout() {
		VerticalLayout weightedResultsLayout = new VerticalLayout();
		for (InstanceType instanceType : service.getResultsForAllBenchmarksForType().keySet()) {
			Double sumWeight = 0d;
			Double sumWeighted = 0d;

			StringBuilder sb = new StringBuilder(instanceType.asString(" | "));
			sb.append(": (");
			MetricsConfiguration metricsConfiguration = uiState.getMetricsConfiguration();
			for (IMetricsType iMetricsType : metricsConfiguration.getRelativeResults().get(instanceType).keySet()) {
				if (!(iMetricsType instanceof CostsStore) || (costsChecked)) {
					Double weight = metricsConfiguration.getWeight(iMetricsType);
					if (weight == null)
						weight = 100d;
					Double relativeResult = metricsConfiguration.getRelativeResults().get(instanceType).get(iMetricsType);

					sb.append(" + ");
					sb.append(weight);
					sb.append(" x ");
					sb.append(relativeResult);

					sumWeight += weight;
					sumWeighted += weight * relativeResult;
				}
			}
			Double sum = sumWeighted / sumWeight;
			sb.append(") / ");
			sb.append(sumWeight);
			sb.append(" = ");
			sb.append(sum);

			Label weightedResultLabel = new Label(sb.toString());
			weightedResultsLayout.addComponent(weightedResultLabel);
		}
		return weightedResultsLayout;
	}

	private VerticalLayout buildWeightResultsLayout(VerticalLayout metricsResultLayout,
	    Class<? extends IInstanceOrderer> instanceOrderer, boolean costsChecked, InstanceType referenceInstance) {
		VerticalLayout weightResultsLayout = new VerticalLayout();
		weightResultsLayout.setSpacing(true);
		weightResultsLayout.setMargin(true);

		Map<InstanceType, Multimap<IMetricsType, IMetricsResult>> resultsForAllMetricsTypesForType =
		    MetricsHelper.combineBenchmarksAndCosts(costsChecked, service.getResultsForAllBenchmarksForType());

		Collection<IMetricsType> metricsTypes = new HashSet<IMetricsType>(service.getAllBenchmarks());
		if (costsChecked)
			metricsTypes.add(CostsStore.getInstance());

		SortedSetMultimap<Double, InstanceType> resultForType;

		for (IMetricsType metricsType : metricsTypes) {
			HorizontalLayout weightResultLayout = new HorizontalLayout();
			weightResultLayout.setWidth("100%");
			weightResultLayout.setSpacing(true);
			weightResultLayout.setMargin(true);

			VerticalLayout resultLabelsLayout = new VerticalLayout();

			Label benchmarkLabel = new Label(metricsType.getName() + ":");
			resultLabelsLayout.addComponent(benchmarkLabel);
			try {

				resultForType =
				    MetricsConfiguration.getInstancesOrderedForMetricsType(
				        metricsType,
				        instanceOrderer,
				        resultsForAllMetricsTypesForType.keySet(),
				        referenceInstance,
				        resultsForAllMetricsTypesForType);

				uiState.getMetricsConfiguration().putRelativeResults(metricsType, resultForType);

				for (Double relativeResult : resultForType.keySet()) {
					for (InstanceType instanceType : resultForType.get(relativeResult)) {
						IMetricsResult result =
						    resultsForAllMetricsTypesForType.get(instanceType).get(metricsType).iterator().next();
						Label resultLabel = buildResultLabel(relativeResult, instanceType, result);
						resultLabelsLayout.addComponent(resultLabel);
					}
				}

			} catch (Exception e) {
				metricsResultLayout.setComponentError(new SystemError("Could not order the metrics type results for "
				    + metricsType.getId() + ": " + e.getMessage()));
			}
			weightResultLayout.addComponent(resultLabelsLayout);

			final Slider weightSlider = buildWeightSlider(metricsType);
			weightResultLayout.addComponent(weightSlider);

			weightResultsLayout.addComponent(weightResultLayout);
		}

		// if (costsChecked) {
		// HorizontalLayout weightResultLayout = new HorizontalLayout();
		// weightResultLayout.setWidth("100%");
		// weightResultLayout.setSpacing(true);
		// weightResultLayout.setMargin(true);
		//
		// VerticalLayout resultLabelsLayout = new VerticalLayout();
		//
		// Label costsLabel = new Label(CostsStore.getInstance().getName() + ":");
		// resultLabelsLayout.addComponent(costsLabel);
		// try {
		//
		// resultForType =
		// MetricsConfiguration.getInstancesOrderedForMetricsType(CostsStore.getInstance(),
		// instanceOrderer, referenceInstance, );
		//
		// for (Double relativeResult : resultForType.keySet()) {
		// Collection<InstanceType> instanceTypes =
		// resultForType.get(relativeResult);
		// for (InstanceType instanceType : instanceTypes) {
		// relativeResults.get(instanceType).put(CostsStore.getInstance(),
		// relativeResult);
		//
		// /* TODO: Remove new instance creation */
		// CostsResult costsResult =
		// new
		// CostsResult(CostsStore.getInstance().getCostsForMonthsRunning(instanceType,
		// 1));
		// Label costsResultLabel = buildResultLabel(relativeResult, instanceType,
		// costsResult);
		// resultLabelsLayout.addComponent(costsResultLabel);
		// }
		// }
		//
		// } catch (InstantiationException | IllegalAccessException e) {
		// metricsResultLayout.setComponentError(new
		// SystemError("Could not order the costs results: " + e.getMessage()));
		// }
		// weightResultLayout.addComponent(resultLabelsLayout);
		//
		// final Slider weightSlider = buildWeightSlider(CostsStore.getInstance());
		// weightResultLayout.addComponent(weightSlider);
		//
		// weightResultsLayout.addComponent(weightResultLayout);
		// }

		return weightResultsLayout;
	}

	private Slider buildWeightSlider(final IMetricsType metricsType) {
		Double weight = uiState.getMetricsConfiguration().getWeight(metricsType);
		if (weight == null)
			weight = 100d;

		final Slider weightSlider = new Slider("How strong should this be weighted?");
		weightSlider.setWidth("100%");
		try {
			weightSlider.setValue(weight);
		} catch (ValueOutOfBoundsException e) {
			e.printStackTrace();
		}
		weightSlider.setMin(0);
		weightSlider.setMax(100);
		weightSlider.setLocale(Locale.US);
		weightSlider.setResolution(1);
		weightSlider.setImmediate(true);
		weightSlider.addListener(new ValueChangeListener() {
			private static final long serialVersionUID = -4202721080880004598L;

			public void valueChange(ValueChangeEvent event) {
				uiState.getMetricsConfiguration().setWeight(metricsType, (Double) weightSlider.getValue());
				VerticalLayout oldWeightedResultsLayout = weightedResultsLayout;
				weightedResultsLayout = buildWeightedResultsLayout();
				metricsResultLayout.replaceComponent(oldWeightedResultsLayout, weightedResultsLayout);
			}
		});
		return weightSlider;
	}

	private VerticalLayout buildWeightResultsLayout(VerticalLayout metricsResultLayout,
	    Class<? extends IInstanceOrderer> instanceOrderer, boolean costsChecked) {
		return buildWeightResultsLayout(metricsResultLayout, instanceOrderer, costsChecked, null);
	}

	private Label buildResultLabel(Double relativeResult, InstanceType instanceType, IMetricsResult metricsResult) {
		Label resultLabel =
		    new Label(instanceType.asString(" | ") + ": " + relativeResult * 100 + "% | "
		        + metricsResult.getValueAsString() + " " + metricsResult.getScale());
		return resultLabel;
	}

	private TextField buildCsvTextField(Class<? extends IInstanceOrderer> instanceOrderer) {
		TextField field = new TextField();

		StringBuilder sb = new StringBuilder();
		sb.append("Provider,Region,HardwareType,Benchmark,Result,Scale,Proportion,");
		sb.append(instanceOrderer.getSimpleName());
		sb.append("\n");

		Map<InstanceType, Multimap<Benchmark, Result>> resultsForAll = service.getResultsForAllBenchmarksForType();

		for (InstanceType instanceType : resultsForAll.keySet()) {
			for (Benchmark benchmark : resultsForAll.get(instanceType).keySet()) {
				for (Result resultForBenchmark : resultsForAll.get(instanceType).get(benchmark)) {
					sb.append(instanceType.getProvider().getId());
					sb.append(",");
					sb.append(instanceType.getRegion().getId());
					sb.append(",");
					sb.append(instanceType.getHardwareType().getId());
					sb.append(",");
					sb.append(benchmark.getId());
					sb.append(",");
					sb.append(resultForBenchmark.getValueAsString());
					sb.append(",");
					sb.append(resultForBenchmark.getScale());
					sb.append(",");
					sb.append(resultForBenchmark.getProportionAsString());
					sb.append(",");
					sb.append(uiState.getMetricsConfiguration().getRelativeResults().get(instanceType).get(benchmark));
					sb.append("\n");
				}
			}

			/* TODO: remove redundant code */
			if (costsChecked) {
				CostsStore monthlyCostResults = CostsStore.getInstance();
				sb.append(instanceType.getProvider().getId());
				sb.append(",");
				sb.append(instanceType.getRegion().getId());
				sb.append(",");
				sb.append(instanceType.getHardwareType().getId());
				sb.append(",");
				sb.append(monthlyCostResults.getId());
				sb.append(",");
				sb.append(monthlyCostResults.getCostsForMonthsRunning(instanceType, 1));
				sb.append(",");
				sb.append("$");
				sb.append(",");
				sb.append(Proportion.LIB.toString());
				sb.append(",");
				sb.append(uiState.getMetricsConfiguration().getRelativeResults().get(instanceType).get(monthlyCostResults));
				sb.append("\n");
			}
		}

		field.setValue(sb.toString());
		return field;
	}
}

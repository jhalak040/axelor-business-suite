/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.talent.web;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.repo.EmployeeRepository;
import com.axelor.apps.talent.db.Appraisal;
import com.axelor.apps.talent.db.repo.AppraisalRepository;
import com.axelor.apps.talent.service.AppraisalService;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;

public class AppraisalController {
	
	@Inject
	private AppraisalRepository appraisalRepo;
	
	@Inject
	private AppraisalService appraisalService;
	
	@Inject
	private EmployeeRepository employeeRepo;
	
	public void send(ActionRequest request, ActionResponse response) {
		
		Appraisal appraisal = request.getContext().asType(Appraisal.class);
		
		appraisal = appraisalRepo.find(appraisal.getId());
		
		appraisalService.send(appraisal);
		
		response.setReload(true);
		
	}
	
	public void cancel(ActionRequest request, ActionResponse response) {
		
		Appraisal appraisal = request.getContext().asType(Appraisal.class);
		
		appraisal = appraisalRepo.find(appraisal.getId());
		
		appraisalService.cancel(appraisal);
		
		response.setReload(true);
		
	}
	
	public void createAppraisals(ActionRequest request, ActionResponse response) {
		
		Context context = request.getContext();
		
		Set<Map<String,Object>> employeeSet = new HashSet<Map<String,Object>>();
		
		employeeSet.addAll((Collection<? extends Map<String, Object>>) context.get("employeeSet"));
		
		Set<Employee> employees = new HashSet<Employee>();
		
		for (Map<String, Object> emp : employeeSet) {
			Long empId = Long.parseLong(emp.get("id").toString());
			employees.add(employeeRepo.find(empId));
		}
		
		Long templateId = Long.parseLong(context.get("templateId").toString());
		
		Appraisal appraisalTemplate = appraisalRepo.find(templateId);
		
		Boolean send = (Boolean) context.get("sendAppraisals");
		
		Set<Long> createdIds = appraisalService.createAppraisals(appraisalTemplate, employees, send);
		
		response.setView(ActionView.define("Appraisal")
				.model(Appraisal.class.getName())
				.add("grid","appraisal-grid")
				.add("form","appraisal-form")
				.domain("self.id in :createdIds")
				.context("createdIds", createdIds)
				.map());
		
		response.setCanClose(true);
		
	}
}
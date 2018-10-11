package com.axelor.apps.gst.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.account.service.invoice.print.InvoicePrintServiceImpl;
import com.axelor.apps.account.web.InvoiceLineController;
import com.axelor.apps.base.web.ProductController;
import com.axelor.apps.businessproject.service.InvoiceServiceProjectImpl;
import com.axelor.apps.gst.service.InvoiceGstPrintService;
import com.axelor.apps.gst.service.InvoiceGstService;
import com.axelor.apps.gst.service.InvoiceLineGstService;
import com.axelor.apps.gst.service.InvoiceLineGstServiceImpl;
import com.axelor.apps.gst.web.InvoiceLineGstController;
import com.axelor.apps.gst.web.ProductGstController;
import com.axelor.apps.supplychain.service.InvoiceLineSupplychainService;

public class GstModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(InvoiceLineSupplychainService.class).to(InvoiceLineGstServiceImpl.class);
    bind(InvoiceLineGstService.class).to(InvoiceLineGstServiceImpl.class);
    bind(InvoiceServiceProjectImpl.class).to(InvoiceGstService.class);
    bind(InvoicePrintServiceImpl.class).to(InvoiceGstPrintService.class);
    bind(InvoiceLineController.class).to(InvoiceLineGstController.class);
    bind(ProductController.class).to(ProductGstController.class);
  }
}

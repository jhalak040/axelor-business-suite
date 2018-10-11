package com.axelor.apps.gst.web;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.service.invoice.InvoiceLineService;
import com.axelor.apps.account.service.invoice.generator.line.InvoiceLineManagement;
import com.axelor.apps.account.web.InvoiceLineController;
import com.axelor.apps.gst.service.InvoiceLineGstService;
import com.axelor.exception.AxelorException;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;
import java.math.BigDecimal;

public class InvoiceLineGstController extends InvoiceLineController {

  @Inject private InvoiceLineGstService invoiceLineGstService;

  @Inject private InvoiceLineService invoiceLineService;

  @Override
  public void compute(ActionRequest request, ActionResponse response) throws AxelorException {

    Context context = request.getContext();

    InvoiceLine invoiceLine = context.asType(InvoiceLine.class);

    Invoice invoice = this.getInvoice(context);

    if (invoice == null || invoiceLine.getPrice() == null || invoiceLine.getQty() == null) {
      return;
    }

    BigDecimal exTaxTotal = BigDecimal.ZERO;
    BigDecimal companyExTaxTotal = BigDecimal.ZERO;
    BigDecimal inTaxTotal = BigDecimal.ZERO;
    BigDecimal companyInTaxTotal = BigDecimal.ZERO;
    BigDecimal priceDiscounted = invoiceLineService.computeDiscount(invoiceLine, invoice);

    response.setValue("priceDiscounted", priceDiscounted);
    response.setAttr(
        "priceDiscounted", "hidden", priceDiscounted.compareTo(invoiceLine.getPrice()) == 0);

    BigDecimal taxRate = BigDecimal.ZERO;
    if (invoiceLine.getTaxLine() != null) {
      taxRate = invoiceLine.getTaxLine().getValue();
      response.setValue("taxRate", taxRate);
      response.setValue("taxCode", invoiceLine.getTaxLine().getTax().getCode());
    }
    invoiceLine = invoiceLineGstService.calculateInvoiceLine(invoice, invoiceLine);

    if (!invoice.getInAti()) {
      exTaxTotal =
          InvoiceLineManagement.computeAmount(
              invoiceLine.getQty(), invoiceLineService.computeDiscount(invoiceLine, invoice));
      inTaxTotal = exTaxTotal.add(exTaxTotal.multiply(taxRate));
    } else {
      inTaxTotal =
          InvoiceLineManagement.computeAmount(
              invoiceLine.getQty(), invoiceLineService.computeDiscount(invoiceLine, invoice));
      exTaxTotal = inTaxTotal.divide(taxRate.add(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP);
    }
    response.setValue("netAmmount", invoiceLine.getNetAmmount());
    response.setValue("igst", invoiceLine.getIgst());
    response.setValue("sgst", invoiceLine.getSgst());
    response.setValue("cgst", invoiceLine.getCgst());
    response.setValue("grossAmount", invoiceLine.getGrossAmount());
    response.setValue("exTaxTotal", exTaxTotal);
    response.setValue("inTaxTotal", inTaxTotal);
    companyExTaxTotal = invoiceLineService.getCompanyExTaxTotal(exTaxTotal, invoice);
    companyInTaxTotal = invoiceLineService.getCompanyExTaxTotal(inTaxTotal, invoice);
    response.setValue("companyInTaxTotal", companyInTaxTotal);
    response.setValue("companyExTaxTotal", companyExTaxTotal);
  }
}

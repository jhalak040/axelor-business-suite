package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.invoice.factory.CancelFactory;
import com.axelor.apps.account.service.invoice.factory.ValidateFactory;
import com.axelor.apps.account.service.invoice.factory.VentilateFactory;
import com.axelor.apps.account.service.invoice.generator.InvoiceGenerator;
import com.axelor.apps.base.service.alarm.AlarmEngineService;
import com.axelor.apps.businessproject.service.InvoiceServiceProjectImpl;
import com.axelor.common.ObjectUtils;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvoiceGstService extends InvoiceServiceProjectImpl {
  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  public InvoiceGstService(
      ValidateFactory validateFactory,
      VentilateFactory ventilateFactory,
      CancelFactory cancelFactory,
      AlarmEngineService<Invoice> alarmEngineService,
      InvoiceRepository invoiceRepo,
      AppAccountService appAccountService) {
    super(
        validateFactory,
        ventilateFactory,
        cancelFactory,
        alarmEngineService,
        invoiceRepo,
        appAccountService);
    // TODO Auto-generated constructor stub
  }

  @Override
  public Invoice compute(final Invoice invoice) throws AxelorException {
    log.debug("Calcul de la facture");

    InvoiceGenerator invoiceGenerator =
        new InvoiceGenerator() {

          @Override
          public Invoice generate() throws AxelorException {

            List<InvoiceLine> invoiceLines = new ArrayList<InvoiceLine>();
            invoiceLines.addAll(invoice.getInvoiceLineList());

            populate(invoice, invoiceLines);
            BigDecimal netAmmount = BigDecimal.ZERO;
            BigDecimal netIgst = BigDecimal.ZERO;
            BigDecimal netRate = BigDecimal.ZERO;
            BigDecimal grossAmmount = BigDecimal.ZERO;
            // BigDecimal taxTotal = BigDecimal.ZERO;
            if (!ObjectUtils.isEmpty(invoice.getInvoiceLineList())) {
              for (InvoiceLine item : invoice.getInvoiceLineList()) {
                netAmmount = netAmmount.add(item.getNetAmmount());
                invoice.setExTaxTotal(netAmmount);
                netRate = netRate.add(item.getCgst());
                invoice.setNetSGST(netRate);
                invoice.setNetCGST(netRate);
                netIgst = netIgst.add(item.getIgst());
                invoice.setNetIGST(netIgst);
                grossAmmount = grossAmmount.add(item.getGrossAmount());
                invoice.setInTaxTotal(grossAmmount);
              }
            }
            return invoice;
          }
        };

    Invoice invoice1 = invoiceGenerator.generate();
    invoice1.setAdvancePaymentInvoiceSet(this.getDefaultAdvancePaymentInvoice(invoice1));
    return invoice1;
  }
}

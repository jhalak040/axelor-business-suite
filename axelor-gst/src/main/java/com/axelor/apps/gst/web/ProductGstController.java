package com.axelor.apps.gst.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.PartnerBaseRepository;
import com.axelor.apps.base.db.repo.ProductBaseRepository;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.gst.report.IReport;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.auth.db.User;
import com.axelor.common.ObjectUtils;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductGstController extends com.axelor.apps.base.web.ProductController {
  @Inject ProductBaseRepository productRepository;
  @Inject PartnerBaseRepository partnerRepo;

  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void productCategory(ActionRequest request, ActionResponse response) {
    Product product = request.getContext().asType(Product.class);
    BigDecimal gstRate = product.getProductCategory().getGstRate();
    response.setValue("gstRate", gstRate);
  }

  public void invoiceCreateOnProduct(ActionRequest request, ActionResponse response) {
    @SuppressWarnings("unchecked")
    List<Integer> idList = (List<Integer>) request.getContext().get("_ids");
    String ids = "";
    if (ObjectUtils.notEmpty(idList)) {
      /*
       * List<Invoice> invoiceList = new ArrayList<>(); List<Partner> partnerlist =
       * partnerRepo.all().fetch(); Invoice invoice = new Invoice();
       */
      List<InvoiceLine> invoiceLineList = new ArrayList<>();
      for (Integer id : idList) {
        ids = ids + id.toString() + ",";
        List<Product> productlist = productRepository.all().filter("self.id= ?", id).fetch();
        for (Product product : productlist) {
          InvoiceLine invoiceLine = new InvoiceLine();
          invoiceLine.setProduct(product);
          invoiceLine.setProductName(product.getName());
          // invoiceLineFromProduct.setItems(product2.getName() + " " +
          // product2.getCode());
          invoiceLine.setGstRate(product.getGstRate());
          invoiceLine.setHsbn(product.getHsbn());
          invoiceLine.setPrice(product.getSalePrice());
          invoiceLine.setUnit(product.getUnit());
          invoiceLineList.add(invoiceLine);
        }
      }
      ids = ids.substring(0, ids.length() - 1);
      request.getContext().put("ids", ids);
      response.setView(
          ActionView.define("form")
              .model("com.axelor.apps.account.db.Invoice")
              .add("form", "invoice-form")
              .context("_invoiceLineList", invoiceLineList)
              .map());
      System.out.println(invoiceLineList);

    } else {
      response.setView(
          ActionView.define("form")
              .model("com.axelor.apps.account.db.Invoice")
              .add("form", "invoice-form")
              .map());
    }
  }

  @SuppressWarnings("unchecked")
  public void printProductCatalog(ActionRequest request, ActionResponse response)
      throws AxelorException {

    User user = Beans.get(UserService.class).getUser();

    int currentYear = Beans.get(AppBaseService.class).getTodayDateTime().getYear();
    String productIds = "";

    List<Integer> lstSelectedProduct = (List<Integer>) request.getContext().get("_ids");

    if (lstSelectedProduct != null) {
      productIds = Joiner.on(",").join(lstSelectedProduct);
    }

    String name = I18n.get("Product Catalog");

    String fileLink =
        ReportFactory.createReport(IReport.PRODUCT_REPORT, name + "-${date}")
            .addParam("UserId", user.getId())
            .addParam("CurrYear", Integer.toString(currentYear))
            .addParam("ProductIds", productIds)
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .generate()
            .getFileLink();

    logger.debug("Printing " + name);

    response.setView(ActionView.define(name).add("html", fileLink).map());
  }
}

package com.loadtrack.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.loadtrack.entity.Payment;
import com.loadtrack.entity.PaymentTransaction;
import com.loadtrack.entity.Receipt;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.PaymentTransactionRepository;
import com.loadtrack.repository.ReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReceiptService {

    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private PaymentService paymentService;
    @Autowired private PaymentTransactionRepository transactionRepository;

    // ── Brand Colors ──────────────────────────────────────────────
    private static final BaseColor PRIMARY      = new BaseColor(41,  128, 185);  // Blue
    private static final BaseColor PRIMARY_DARK = new BaseColor(21,  67,  96);   // Dark Blue
    private static final BaseColor SUCCESS      = new BaseColor(39,  174, 96);   // Green
    private static final BaseColor DANGER       = new BaseColor(231, 76,  60);   // Red
    private static final BaseColor WARNING      = new BaseColor(243, 156, 18);   // Orange
    private static final BaseColor LIGHT_BG     = new BaseColor(236, 240, 241);  // Light Gray
    private static final BaseColor HEADER_BG    = new BaseColor(44,  62,  80);   // Dark Header
    private static final BaseColor WHITE        = BaseColor.WHITE;
    private static final BaseColor TEXT_DARK    = new BaseColor(44,  62,  80);
    private static final BaseColor TEXT_MUTED   = new BaseColor(127, 140, 141);

    // ── Fonts ──────────────────────────────────────────────────────
    private static final Font FONT_TITLE        = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   WHITE);
    private static final Font FONT_SUBTITLE     = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(189, 195, 199));
    private static final Font FONT_SECTION      = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   PRIMARY_DARK);
    private static final Font FONT_LABEL        = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   TEXT_MUTED);
    private static final Font FONT_VALUE        = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
    private static final Font FONT_VALUE_BOLD   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   TEXT_DARK);
    private static final Font FONT_AMOUNT_BIG   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   SUCCESS);
    private static final Font FONT_TABLE_HEADER = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   WHITE);
    private static final Font FONT_TABLE_CELL   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, TEXT_DARK);
    private static final Font FONT_PAID         = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   SUCCESS);
    private static final Font FONT_BALANCE      = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DANGER);
    private static final Font FONT_FOOTER       = new Font(Font.FontFamily.HELVETICA, 8,  Font.ITALIC, TEXT_MUTED);
    private static final Font FONT_STATUS_PAID  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   SUCCESS);
    private static final Font FONT_STATUS_PART  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   WARNING);
    private static final Font FONT_STATUS_UNPAID= new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   DANGER);

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public List<Receipt> getReceiptsByPayment(Integer paymentId) {
        return receiptRepository.findByPaymentId(paymentId);
    }

    public Receipt getReceiptById(Integer id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + id));
    }

    public byte[] generateReceiptPdf(Integer receiptId) throws Exception {
        Receipt receipt = getReceiptById(receiptId);
        Payment payment = receipt.getPayment();
        List<PaymentTransaction> transactions =
                transactionRepository.findByPaymentIdOrderByPaidAtAsc(payment.getId());

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        // Page border event
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter w, Document d) {
                PdfContentByte cb = w.getDirectContent();
                cb.setColorStroke(LIGHT_BG);
                cb.setLineWidth(1.5f);
                cb.rectangle(20, 20, d.getPageSize().getWidth() - 40, d.getPageSize().getHeight() - 40);
                cb.stroke();
            }
        });

        doc.open();

        // ── 1. HEADER BANNER ─────────────────────────────────────
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60, 40});

        // Left: Brand
        PdfPCell brandCell = new PdfPCell();
        brandCell.setBackgroundColor(HEADER_BG);
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.setPadding(20);

        Paragraph brand = new Paragraph();
        brand.add(new Chunk("🚛  LoadTrack", FONT_TITLE));
        brand.add(Chunk.NEWLINE);
        brand.add(new Chunk("Truck Operations Management System", FONT_SUBTITLE));
        brandCell.addElement(brand);
        header.addCell(brandCell);

        // Right: Receipt Info
        PdfPCell receiptInfoCell = new PdfPCell();
        receiptInfoCell.setBackgroundColor(PRIMARY);
        receiptInfoCell.setBorder(Rectangle.NO_BORDER);
        receiptInfoCell.setPadding(20);
        receiptInfoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font receiptTitleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, WHITE);
        Font receiptNumFont   = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(189, 195, 199));
        Font receiptValFont   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, WHITE);

        Paragraph receiptInfo = new Paragraph();
        receiptInfo.setAlignment(Element.ALIGN_RIGHT);
        receiptInfo.add(new Chunk("PAYMENT RECEIPT", receiptTitleFont));
        receiptInfo.add(Chunk.NEWLINE);
        receiptInfo.add(new Chunk("Receipt No: ", receiptNumFont));
        receiptInfo.add(new Chunk(receipt.getReceiptNumber(), receiptValFont));
        receiptInfo.add(Chunk.NEWLINE);
        receiptInfo.add(new Chunk("Generated: ", receiptNumFont));
        receiptInfo.add(new Chunk(receipt.getGeneratedAt().format(DATETIME_FMT), receiptValFont));
        receiptInfoCell.addElement(receiptInfo);
        header.addCell(receiptInfoCell);

        doc.add(header);
        doc.add(spacer(12));

        // ── 2. DEALER & TRIP INFO ─────────────────────────────────
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{50, 50});
        infoTable.setSpacingBefore(4);

        // Dealer Info box
        PdfPCell dealerBox = buildInfoBox("DEALER INFORMATION",
                new String[]{"Name", "Phone", "Address"},
                new String[]{
                        payment.getTrip().getDealer().getName(),
                        payment.getTrip().getDealer().getPhone(),
                        nvl(payment.getTrip().getDealer().getAddress())
                });
        infoTable.addCell(dealerBox);

        // Trip Info box
        PdfPCell tripBox = buildInfoBox("TRIP DETAILS",
                new String[]{"Trip ID", "Trip Date", "Truck", "Driver", "Sand Type", "Tons"},
                new String[]{
                        "#" + payment.getTrip().getId(),
                        payment.getTrip().getTripDate().format(DATE_FMT),
                        payment.getTrip().getTruck().getTruckNumber(),
                        payment.getTrip().getDriver().getName(),
                        payment.getTrip().getSandType().getName(),
                        payment.getTrip().getTons() + " tons"
                });
        infoTable.addCell(tripBox);

        doc.add(infoTable);
        doc.add(spacer(12));

        // ── 3. ROUTE ─────────────────────────────────────────────
        PdfPTable routeTable = new PdfPTable(3);
        routeTable.setWidthPercentage(100);
        routeTable.setWidths(new float[]{40, 20, 40});

        PdfPCell fromCell = new PdfPCell();
        fromCell.setBackgroundColor(new BaseColor(232, 245, 233));
        fromCell.setBorder(Rectangle.NO_BORDER);
        fromCell.setPadding(12);
        fromCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph fromP = new Paragraph();
        fromP.setAlignment(Element.ALIGN_CENTER);
        fromP.add(new Chunk("SOURCE\n", FONT_LABEL));
        fromP.add(new Chunk(payment.getTrip().getSourceLocation(),
                new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, SUCCESS)));
        fromCell.addElement(fromP);

        PdfPCell arrowCell = new PdfPCell(new Phrase("  ➜  ",
                new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, PRIMARY)));
        arrowCell.setBorder(Rectangle.NO_BORDER);
        arrowCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        arrowCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        arrowCell.setPadding(12);

        PdfPCell toCell = new PdfPCell();
        toCell.setBackgroundColor(new BaseColor(232, 244, 253));
        toCell.setBorder(Rectangle.NO_BORDER);
        toCell.setPadding(12);
        toCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph toP = new Paragraph();
        toP.setAlignment(Element.ALIGN_CENTER);
        toP.add(new Chunk("DESTINATION\n", FONT_LABEL));
        toP.add(new Chunk(payment.getTrip().getDestinationLocation(),
                new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, PRIMARY)));
        toCell.addElement(toP);

        routeTable.addCell(fromCell);
        routeTable.addCell(arrowCell);
        routeTable.addCell(toCell);
        doc.add(routeTable);
        doc.add(spacer(12));

        // ── 4. PAYMENT SUMMARY CARDS ──────────────────────────────
        doc.add(sectionTitle("PAYMENT SUMMARY"));

        PdfPTable summaryCards = new PdfPTable(4);
        summaryCards.setWidthPercentage(100);
        summaryCards.setSpacingBefore(6);

        summaryCards.addCell(buildSummaryCard("TRIP AMOUNT",
                "₹" + formatAmount(payment.getOriginalAmount()), LIGHT_BG, TEXT_DARK));
        summaryCards.addCell(buildSummaryCard("INTEREST",
                "₹" + formatAmount(payment.getInterestAmount()),
                payment.getInterestAmount().doubleValue() > 0
                        ? new BaseColor(255, 243, 205) : LIGHT_BG,
                payment.getInterestAmount().doubleValue() > 0 ? WARNING : TEXT_MUTED));
        summaryCards.addCell(buildSummaryCard("TOTAL DUE",
                "₹" + formatAmount(payment.getFinalAmount()),
                new BaseColor(232, 244, 253), PRIMARY));
        summaryCards.addCell(buildSummaryCard("AMOUNT PAID",
                "₹" + formatAmount(payment.getPaidAmount()),
                new BaseColor(232, 245, 233), SUCCESS));

        doc.add(summaryCards);
        doc.add(spacer(8));

        // Remaining amount + Status
        PdfPTable statusRow = new PdfPTable(2);
        statusRow.setWidthPercentage(100);

        double remaining = payment.getFinalAmount().doubleValue() - payment.getPaidAmount().doubleValue();
        PdfPCell remainingCell = new PdfPCell();
        remainingCell.setBackgroundColor(remaining > 0 ? new BaseColor(253, 237, 236) : new BaseColor(232, 245, 233));
        remainingCell.setBorder(Rectangle.NO_BORDER);
        remainingCell.setPadding(12);
        Paragraph remP = new Paragraph();
        remP.add(new Chunk("REMAINING BALANCE:  ", FONT_LABEL));
        remP.add(new Chunk("₹" + formatAmount(payment.getFinalAmount().subtract(payment.getPaidAmount())),
                remaining > 0
                        ? new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, DANGER)
                        : new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, SUCCESS)));
        remainingCell.addElement(remP);
        statusRow.addCell(remainingCell);

        PdfPCell statusCell = new PdfPCell();
        statusCell.setBorder(Rectangle.NO_BORDER);
        statusCell.setPadding(12);
        statusCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Font statusFont = switch (payment.getPaymentStatus()) {
            case PAID    -> FONT_STATUS_PAID;
            case PARTIAL -> FONT_STATUS_PART;
            default      -> FONT_STATUS_UNPAID;
        };
        String statusLabel = switch (payment.getPaymentStatus()) {
            case PAID    -> "✔  FULLY PAID";
            case PARTIAL -> "◑  PARTIALLY PAID";
            default      -> "✘  UNPAID";
        };
        Paragraph statusP = new Paragraph(statusLabel, statusFont);
        statusP.setAlignment(Element.ALIGN_RIGHT);
        statusCell.addElement(statusP);
        statusRow.addCell(statusCell);
        doc.add(statusRow);
        doc.add(spacer(14));

        // ── 5. TRANSACTION LOG ────────────────────────────────────
        doc.add(sectionTitle("TRANSACTION HISTORY"));

        if (transactions.isEmpty()) {
            Paragraph noTxn = new Paragraph("No payment transactions recorded yet.", FONT_VALUE);
            noTxn.setSpacingBefore(6);
            doc.add(noTxn);
        } else {
            PdfPTable txnTable = new PdfPTable(5);
            txnTable.setWidthPercentage(100);
            txnTable.setWidths(new float[]{8, 22, 20, 30, 20});
            txnTable.setSpacingBefore(6);

            // Table header
            String[] txnHeaders = {"#", "Date & Time", "Amount Paid", "Remarks", "Balance After"};
            for (String h : txnHeaders) {
                PdfPCell hCell = new PdfPCell(new Phrase(h, FONT_TABLE_HEADER));
                hCell.setBackgroundColor(HEADER_BG);
                hCell.setPadding(8);
                hCell.setBorder(Rectangle.NO_BORDER);
                hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                txnTable.addCell(hCell);
            }

            // Table rows
            int txnNum = 1;
            for (PaymentTransaction txn : transactions) {
                boolean isLast = txnNum == transactions.size();
                BaseColor rowBg = txnNum % 2 == 0 ? new BaseColor(248, 249, 250) : WHITE;

                // # column
                PdfPCell numCell = new PdfPCell(new Phrase(String.valueOf(txnNum), FONT_TABLE_CELL));
                numCell.setBackgroundColor(rowBg);
                numCell.setPadding(7);
                numCell.setBorder(Rectangle.BOTTOM);
                numCell.setBorderColor(LIGHT_BG);
                numCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                txnTable.addCell(numCell);

                // Date
                PdfPCell dateCell = new PdfPCell(new Phrase(
                        txn.getPaidAt().format(DATETIME_FMT), FONT_TABLE_CELL));
                dateCell.setBackgroundColor(rowBg);
                dateCell.setPadding(7);
                dateCell.setBorder(Rectangle.BOTTOM);
                dateCell.setBorderColor(LIGHT_BG);
                txnTable.addCell(dateCell);

                // Amount paid
                PdfPCell amtCell = new PdfPCell(new Phrase(
                        "₹" + formatAmount(txn.getAmountPaid()), FONT_PAID));
                amtCell.setBackgroundColor(rowBg);
                amtCell.setPadding(7);
                amtCell.setBorder(Rectangle.BOTTOM);
                amtCell.setBorderColor(LIGHT_BG);
                amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                txnTable.addCell(amtCell);

                // Remarks
                PdfPCell remCell = new PdfPCell(new Phrase(
                        nvl(txn.getRemarks()), FONT_TABLE_CELL));
                remCell.setBackgroundColor(rowBg);
                remCell.setPadding(7);
                remCell.setBorder(Rectangle.BOTTOM);
                remCell.setBorderColor(LIGHT_BG);
                txnTable.addCell(remCell);

                // Balance after
                double bal = txn.getBalanceAfter().doubleValue();
                Font balFont = bal == 0 ? FONT_PAID : FONT_BALANCE;
                String balText = bal == 0 ? "₹0  ✔" : "₹" + formatAmount(txn.getBalanceAfter());
                PdfPCell balCell = new PdfPCell(new Phrase(balText, balFont));
                balCell.setBackgroundColor(isLast && bal == 0
                        ? new BaseColor(232, 245, 233) : rowBg);
                balCell.setPadding(7);
                balCell.setBorder(Rectangle.BOTTOM);
                balCell.setBorderColor(LIGHT_BG);
                balCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                txnTable.addCell(balCell);

                txnNum++;
            }

            // Totals row
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL PAID", FONT_TABLE_HEADER));
            totalLabelCell.setColspan(2);
            totalLabelCell.setBackgroundColor(PRIMARY_DARK);
            totalLabelCell.setPadding(8);
            totalLabelCell.setBorder(Rectangle.NO_BORDER);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            txnTable.addCell(totalLabelCell);

            PdfPCell totalAmtCell = new PdfPCell(new Phrase(
                    "₹" + formatAmount(payment.getPaidAmount()),
                    new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, SUCCESS)));
            totalAmtCell.setBackgroundColor(PRIMARY_DARK);
            totalAmtCell.setPadding(8);
            totalAmtCell.setBorder(Rectangle.NO_BORDER);
            totalAmtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            txnTable.addCell(totalAmtCell);

            PdfPCell totalRemLabelCell = new PdfPCell(new Phrase("REMAINING", FONT_TABLE_HEADER));
            totalRemLabelCell.setBackgroundColor(PRIMARY_DARK);
            totalRemLabelCell.setPadding(8);
            totalRemLabelCell.setBorder(Rectangle.NO_BORDER);
            txnTable.addCell(totalRemLabelCell);

            double rem = payment.getFinalAmount().doubleValue() - payment.getPaidAmount().doubleValue();
            Font remFont = rem == 0
                    ? new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, SUCCESS)
                    : new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, DANGER);
            PdfPCell totalRemCell = new PdfPCell(new Phrase(
                    rem == 0 ? "CLEARED ✔" : "₹" + String.format("%.2f", rem), remFont));
            totalRemCell.setBackgroundColor(PRIMARY_DARK);
            totalRemCell.setPadding(8);
            totalRemCell.setBorder(Rectangle.NO_BORDER);
            totalRemCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            txnTable.addCell(totalRemCell);

            doc.add(txnTable);
        }

        doc.add(spacer(16));

        // ── 6. DUE DATE & INTEREST NOTE ──────────────────────────
        if (payment.getDueDate() != null || payment.getInterestAmount().doubleValue() > 0) {
            PdfPTable noteTable = new PdfPTable(1);
            noteTable.setWidthPercentage(100);
            PdfPCell noteCell = new PdfPCell();
            noteCell.setBackgroundColor(new BaseColor(255, 243, 205));
            noteCell.setBorder(Rectangle.NO_BORDER);
            noteCell.setPadding(10);
            Paragraph noteP = new Paragraph();
            noteP.add(new Chunk("⚠  NOTE:  ", new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, WARNING)));
            if (payment.getDueDate() != null) {
                noteP.add(new Chunk("Payment due date: " + payment.getDueDate().format(DATE_FMT) + ".  ",
                        new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK)));
            }
            if (payment.getInterestAmount().doubleValue() > 0) {
                noteP.add(new Chunk("Interest of ₹" + formatAmount(payment.getInterestAmount())
                        + " has been applied for delayed payment.",
                        new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, DANGER)));
            }
            noteCell.addElement(noteP);
            noteTable.addCell(noteCell);
            doc.add(noteTable);
            doc.add(spacer(12));
        }

        // ── 7. FOOTER ─────────────────────────────────────────────
        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);

        PdfPCell footerLeft = new PdfPCell();
        footerLeft.setBorder(Rectangle.TOP);
        footerLeft.setBorderColor(LIGHT_BG);
        footerLeft.setPaddingTop(10);
        footerLeft.setPaddingBottom(6);
        footerLeft.addElement(new Paragraph("Thank you for your business!", FONT_FOOTER));
        footerLeft.addElement(new Paragraph("LoadTrack – Truck Operations Management System", FONT_FOOTER));
        footer.addCell(footerLeft);

        PdfPCell footerRight = new PdfPCell();
        footerRight.setBorder(Rectangle.TOP);
        footerRight.setBorderColor(LIGHT_BG);
        footerRight.setPaddingTop(10);
        footerRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph footerRightP = new Paragraph();
        footerRightP.setAlignment(Element.ALIGN_RIGHT);
        footerRightP.add(new Chunk("This is a computer-generated receipt.\nNo signature required.", FONT_FOOTER));
        footerRight.addElement(footerRightP);
        footer.addCell(footerRight);

        doc.add(footer);
        doc.close();

        return out.toByteArray();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Paragraph sectionTitle(String title) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(title, FONT_SECTION));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(PRIMARY);
        c.setBorderWidth(2);
        c.setPaddingBottom(4);
        c.setPaddingTop(2);
        t.addCell(c);
        Paragraph p = new Paragraph();
        p.add(new Chunk(""));
        return p;
    }

    private Element spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(height);
        return p;
    }

    private PdfPCell buildInfoBox(String title, String[] labels, String[] values) {
        PdfPCell box = new PdfPCell();
        box.setBorder(Rectangle.BOX);
        box.setBorderColor(LIGHT_BG);
        box.setPadding(12);

        Paragraph titleP = new Paragraph(title, FONT_SECTION);
        titleP.setSpacingAfter(6);
        box.addElement(titleP);

        for (int i = 0; i < labels.length; i++) {
            Paragraph row = new Paragraph();
            row.add(new Chunk(labels[i] + ":  ", FONT_LABEL));
            row.add(new Chunk(values[i], FONT_VALUE_BOLD));
            row.setSpacingAfter(3);
            box.addElement(row);
        }
        return box;
    }

    private PdfPCell buildSummaryCard(String label, String value, BaseColor bg, BaseColor valueColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk(label + "\n", FONT_LABEL));
        p.add(new Chunk(value, new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, valueColor)));
        cell.addElement(p);
        return cell;
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount.doubleValue());
    }

    private String nvl(String val) {
        return (val == null || val.isBlank()) ? "—" : val;
    }
}

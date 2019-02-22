package broker.loanbroker;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import shared.event.BankReplyListener;
import shared.event.LoanRequestListener;
import shared.gateway.BankAppGateway;
import shared.gateway.LoanClientAppGateway;
import shared.model.bank.BankInterestReply;
import shared.model.bank.BankInterestRequest;
import shared.model.loan.LoanReply;
import shared.model.loan.LoanRequest;

public class LoanBrokerFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private DefaultListModel<loanbroker.JListLine> listModel = new DefaultListModel<loanbroker.JListLine>();
	private JList<loanbroker.JListLine> list;

	public LoanClientAppGateway loanClientGateway;
	public BankAppGateway bankAppGateway;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LoanBrokerFrame frame = new LoanBrokerFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public LoanBrokerFrame() {
		setup();
		setTitle("Loan Broker");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{46, 31, 86, 30, 89, 0};
		gbl_contentPane.rowHeights = new int[]{233, 23, 0};
		gbl_contentPane.columnWeights = new double[]{1.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);
		
		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.gridwidth = 7;
		gbc_scrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		contentPane.add(scrollPane, gbc_scrollPane);
		
		list = new JList<loanbroker.JListLine>(listModel);
		scrollPane.setViewportView(list);		
	}
	
	 private loanbroker.JListLine getRequestReply(LoanRequest request){
	     
	     for (int i = 0; i < listModel.getSize(); i++){
	    	 loanbroker.JListLine rr =listModel.get(i);
	    	 if (rr.getLoanRequest() == request){
	    		 return rr;
	    	 }
	     }
	     
	     return null;
	}
	
	public void add(LoanRequest loanRequest){		
		listModel.addElement(new loanbroker.JListLine(loanRequest));
	}
	

	public void add(LoanRequest loanRequest, BankInterestRequest bankRequest){
		loanbroker.JListLine rr = getRequestReply(loanRequest);
		if (rr!= null && bankRequest != null){
			rr.setBankRequest(bankRequest);
            list.repaint();
		}		
	}
	
	public void add(LoanRequest loanRequest, BankInterestReply bankReply){
		loanbroker.JListLine rr = getRequestReply(loanRequest);
		if (rr!= null && bankReply != null){
			rr.setBankReply(bankReply);
            list.repaint();
		}		
	}

	public void setup() {
		loanClientGateway = new LoanClientAppGateway(
				"BrokerClient",
				"ClientBroker");
		bankAppGateway = new BankAppGateway (
				"BrokerBank",
				"BankBroker");

		loanClientGateway.addLoanRequestListener(new LoanRequestListener() {
			@Override
			public void onLoanRequest(LoanRequest loanRequest, String correlation) {
				add(loanRequest);

				BankInterestRequest request = new BankInterestRequest();
				request.setAmount(loanRequest.getAmount());
				request.setTime(loanRequest.getTime());

				add(loanRequest, request);

				bankAppGateway.sendBankRequest(loanRequest, request, correlation);
			}
		});

		bankAppGateway.addReplyListneer(new BankReplyListener() {
			@Override
			public void onBankReply(LoanRequest request, BankInterestReply reply, String correlation) {
				System.out.println("Received bankreply: " +  reply.toString());

				add(request, reply);

				LoanReply loanReply = new LoanReply();
				loanReply.setInterest(reply.getInterest());
				loanReply.setQuoteID(reply.getQuoteId());

				loanClientGateway.sendLoanReply(loanReply, correlation);
			}
		});
	}
}

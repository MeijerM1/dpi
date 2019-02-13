package broker.loanbroker;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import shared.messaging.Messenger;
import shared.model.bank.BankInterestReply;
import shared.model.bank.BankInterestRequest;
import shared.model.loan.LoanReply;
import shared.model.loan.LoanRequest;

public class LoanBrokerFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private DefaultListModel<loanbroker.JListLine> listModel = new DefaultListModel<loanbroker.JListLine>();
	private JList<loanbroker.JListLine> list;

	Messenger loanReceiver = new Messenger(true, "LoanBroker");
	Messenger loanSender = new Messenger(false, "LoanReply");

	Messenger interestSender = new Messenger(false, "InterestBroker");
	Messenger interestReceiver = new Messenger(true, "InterestReply");

	private Map<String, LoanRequest> requests = new HashMap<>();

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
		loanReceiver.addReceiver(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				System.out.println("Received message: " + message);
				ObjectMessage object = (ObjectMessage) message;
				try {
					LoanRequest request = (LoanRequest) object.getObject();
					add(request);

					BankInterestRequest bankRequest = new BankInterestRequest(request.getAmount(), request.getTime());
					add(request, bankRequest);

					requests.put(message.getJMSCorrelationID(), request);

					Message msg = interestSender.createMessage(bankRequest);
					msg.setJMSCorrelationID(message.getJMSCorrelationID());

					interestSender.sendMessage(msg);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		});

		interestReceiver.addReceiver(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				ObjectMessage object = (ObjectMessage) message;

				try {
					BankInterestReply reply = (BankInterestReply) object.getObject();
					LoanReply loanReply = new LoanReply(reply.getInterest(), reply.getQuoteId());

					add(requests.get(message.getJMSCorrelationID()) ,reply);

					Message msg = loanSender.createMessage(loanReply);
					msg.setJMSCorrelationID(message.getJMSCorrelationID());
					loanSender.sendMessage(msg);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		});
	}
}

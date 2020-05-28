package farsight.utils.services;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import com.wm.app.b2b.server.InvokeState;
import com.wm.app.b2b.server.ServiceException;
import com.wm.app.b2b.server.debugger.BreakPointManager;
import com.wm.app.b2b.server.debugger.BreakPointSession;
import com.wm.app.b2b.server.debugger.BreakPointSessionImpl;
import com.wm.app.b2b.server.ns.Namespace;
import com.wm.lang.flow.FlowState;
import com.wm.lang.ns.NSService;
import com.wm.lang.ns.NSServiceType;

public class InvokeUtils {
	
	private static final String PRIVATE_DATA_PREFIX = "$ESB$";
	
	public static void setPrivateData(String key, Object value) {
		key = PRIVATE_DATA_PREFIX + key;
		InvokeState.getCurrentState().setPrivateData(key, value);
	}
	
	public static Object getPrivateData(String key) {
		key = PRIVATE_DATA_PREFIX + key;
		return InvokeState.getCurrentState().getPrivateData(key);
	}
	
	public static void removePrivateData(String key) {
		key = PRIVATE_DATA_PREFIX + key;
		InvokeState.getCurrentState().removePrivateData(key);		
	}
	
	public static NSService getCallingService(int level) {
		LinkedList<NSService> stack = getCallStack();
		int pos = stack.size() - 1 - level;
		if(pos < 0)
			return null;
		
		return stack.get(pos);
	}
		
	public static boolean isDebugging() {
		return InvokeState.getCurrentState().getDebuggerSessionId() != null;
	}
	
	public static LinkedList<NSService> getCallStack() {
		return (isDebugging()) ? createDebugCallStack() : getCallStackFromInvokeState();
	}
	

	public static NSService getRootService() {
		LinkedList<NSService> stack = getCallStack();
		if(stack == null)
			return null;
		return stack.peekFirst();
	}
	
	private static LinkedList<NSService> getCallStackFromFlowState(FlowState state) {
		LinkedList<NSService> stack = new LinkedList<>();
		Namespace ns = Namespace.current();
		while(state != null) {
			try {
				String nsName = state.current().getNSName();
				stack.addFirst((NSService) ns.getNode(nsName));
				state = state.getParent();
			} catch(Exception e) {
				//simply ignore and stop
				state = null;
			}
		}
		return stack;
	}
	
	@SuppressWarnings("unchecked")
	private static LinkedList<NSService> getCallStackFromInvokeState() {
		LinkedList<NSService> stack = new LinkedList<>();
		stack.addAll(InvokeState.getCurrentState().getCallStack());
		return stack;
	}
	
	@SuppressWarnings("unchecked")
	private static LinkedList<NSService> createDebugCallStack() {
		//get stack beyond flowDebugger
		LinkedList<NSService> stack = new LinkedList<>();
		stack.addAll((Stack<NSService>) InvokeState.getCurrentState().getCallStack());
		stack.pop(); //remove flowDebugger
		
		//check if top level service is java
		if(stack.size() > 0 && stack.peekFirst().getServiceType().getType().equals(NSServiceType.SVC_JAVA)) {
			//if stack stops at java service, we need to find the longest stack from the current debug session
			try {
				String debugSessionID = InvokeState.getCurrentState().getDebuggerSessionId();
				BreakPointSession session = BreakPointManager.getSession(debugSessionID);
				String[] ids = getFlowStateIDs(session);
				
				LinkedList<NSService> head = null;
				for(int i = 0; i < ids.length; i++) {
					LinkedList<NSService> candidate = getCallStackFromFlowState(session.getFlowState(ids[i]));
					if(head == null || head.size() < candidate.size())
						head = candidate;
				}
				
				if(head == null) {
					stack.addFirst(null); //means generation of debug stack failed to get complete stack!
				} else {
					head.addAll(stack);
					stack = head;
				}
				
			} catch (Exception e) {
				stack.addFirst(null); //means generation of debug stack failed to get complete stack!
			}
			
		} else {
			FlowState state = InvokeState.getCurrentState().getFlowState();
			Namespace ns = Namespace.current();
			while(state != null) {
				try {
					String nsName = state.current().getNSName();
					stack.addFirst((NSService) ns.getNode(nsName));
					state = state.getParent();
				} catch(Exception e) {
					//simply ignore and stop
					state = null;
				}
			}
		}
		//convert to stack
		return stack;
	}
	
	@SuppressWarnings("unchecked")
	private static String[] getFlowStateIDs(BreakPointSession session) throws ServiceException {
		try {
			Field field = BreakPointSessionImpl.class.getDeclaredField("_flowStates");
			field.setAccessible(true);
			Map<String, FlowState> map = (Map<String, FlowState>) field.get(session);
			return map.keySet().toArray(new String[0]);
			
		} catch (Exception e) {
			throw new ServiceException("Cannot read FlowState IDs in BreakPoint session! Reason: " + e.getMessage());
		}
	}


}

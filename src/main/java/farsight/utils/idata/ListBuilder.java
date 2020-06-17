package farsight.utils.idata;

import java.util.LinkedList;

import com.wm.data.IData;

public class ListBuilder {
	private LinkedList<DataBuilder> list = new LinkedList<>();
	private DataBuilder template = null;
	
	public ListBuilder(IData template) {
		this.template = DataBuilder.wrap(template);
	}

	public ListBuilder() {
	}
	
	public static ListBuilder create() {
		return new ListBuilder();
	}

	public DataBuilder add() {
		DataBuilder builder = template == null ? DataBuilder.create() : template.asClone(true);
		list.add(builder);
		return builder;
	}
	
	public ListBuilder add(DataBuilder builder) {
		if(template == null) {
			list.add(builder);
		} else {
			template.asClone(true).merge(builder.build(), true);
		}
		return this;
	}
	
	public ListBuilder add(IData data) {
		list.add(DataBuilder.wrap(data));
		return this;
	}
	
	public IData[] build() {
		IData[] finalList = new IData[list.size()];
		int pos = 0;
		for(DataBuilder item: list) {
			finalList[pos++] = item.build();
		}
		return finalList;
	}
}

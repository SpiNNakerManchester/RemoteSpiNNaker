package uk.ac.manchester.cs.spinnaker.jobprocess;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="provenance_data_items")
public class ProvenanceDataItems {

    @XmlAttribute
    private String name;

    @XmlElement(name="provenance_data_items")
    private List<ProvenanceDataItems> provenanceDataItems = new ArrayList<>();

    @XmlElement(name="provenance_data_item")
    private List<ProvenanceDataItem> provenanceDataItem = new ArrayList<>();

    public String getName() {
        return name;
    }

    public List<ProvenanceDataItems> getProvenanceDataItems() {
        return provenanceDataItems;
    }

    public List<ProvenanceDataItem> getProvenanceDataItem() {
        return provenanceDataItem;
    }

    private static void print(ProvenanceDataItems items, String prefix) {
        String myPrefix = prefix + items.getName();
        for (ProvenanceDataItems subItems : items.getProvenanceDataItems()) {
            print(subItems, myPrefix + "/");
        }
        for (ProvenanceDataItem subItem : items.getProvenanceDataItem()) {
            System.err.println(
                myPrefix + "/" + subItem.getName() + ":" + subItem.getValue());
        }
    }

    public static void main(String[] args) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(
            ProvenanceDataItems.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        ProvenanceDataItems items = (ProvenanceDataItems)
            jaxbUnmarshaller.unmarshal(
                new java.io.File("router_provenance.xml"));
        print(items, "");
    }

}

package it.reactive.muskel.internal.classloader.domain;

import java.io.IOException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse implements DataSerializable {

    private String resourceName;
    private byte[] bytes;

    public ResourceResponse(String resourceName) {
	this(resourceName, null);
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
	out.writeUTF(resourceName);
	out.writeByteArray(bytes);

    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
	this.resourceName = in.readUTF();
	this.bytes = in.readByteArray();
    }
}

/******************************************************************************/
/* This file is generated by the templates/template.rb script and should not  */
/* be modified manually. See                                                  */
/* templates/java/org/yarp/Loader.java.erb                                    */
/* if you are looking to modify the                                           */
/* template                                                                   */
/******************************************************************************/
package org.yarp;

import org.yarp.ParseResult;

import java.lang.Short;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

// GENERATED BY Loader.java.erb
// @formatter:off
public class Loader {

    public static ParseResult load(byte[] serialized, Nodes.Source source) {
        return new Loader(serialized, source).load();
    }

    private static final class ConstantPool {

        private final byte[] source;
        private final int bufferOffset;
        private final byte[][] cache;

        ConstantPool(byte[] source, int bufferOffset, int length) {
            this.source = source;
            this.bufferOffset = bufferOffset;
            cache = new byte[length][];
        }

        byte[] get(ByteBuffer buffer, int oneBasedIndex) {
            int index = oneBasedIndex - 1;
            byte[] constant = cache[index];
            if (constant == null) {
                int offset = bufferOffset + index * 8;
                int start = buffer.getInt(offset);
                int length = buffer.getInt(offset + 4);

                constant = new byte[length];
                System.arraycopy(source, start, constant, 0, length);
                cache[index] = constant;
            }
            return constant;
        }

    }

    private final ByteBuffer buffer;
    private ConstantPool constantPool;
    private final Nodes.Source source;

    private Loader(byte[] serialized, Nodes.Source source) {
        this.buffer = ByteBuffer.wrap(serialized).order(ByteOrder.nativeOrder());
        this.source = source;
    }

    private ParseResult load() {
        expect((byte) 'Y');
        expect((byte) 'A');
        expect((byte) 'R');
        expect((byte) 'P');

        expect((byte) 0);
        expect((byte) 9);
        expect((byte) 0);

        // This loads the name of the encoding. We don't actually do anything
        // with it just yet.
        int encodingLength = loadVarInt();
        byte[] encodingName = new byte[encodingLength];
        buffer.get(encodingName);

        ParseResult.Comment[] comments = loadComments();
        ParseResult.Error[] errors = loadSyntaxErrors();
        ParseResult.Warning[] warnings = loadWarnings();

        int constantPoolBufferOffset = buffer.getInt();
        int constantPoolLength = loadVarInt();
        this.constantPool = new ConstantPool(source.bytes, constantPoolBufferOffset, constantPoolLength);

        Nodes.Node node = loadNode();

        int left = constantPoolBufferOffset - buffer.position();
        if (left != 0) {
            throw new Error("Expected to consume all bytes while deserializing but there were " + left + " bytes left");
        }

        boolean[] newlineMarked = new boolean[1 + source.getLineCount()];
        MarkNewlinesVisitor visitor = new MarkNewlinesVisitor(source, newlineMarked);
        node.accept(visitor);

        return new ParseResult(node, comments, errors, warnings);
    }

    private byte[] loadEmbeddedString() {
        int length = loadVarInt();
        byte[] string = new byte[length];
        buffer.get(string);
        return string;
    }

    private byte[] loadString() {
        switch (buffer.get()) {
            case 1:
                int start = loadVarInt();
                int length = loadVarInt();
                byte[] string = new byte[length];
                System.arraycopy(source.bytes, start, string, 0, length);
                return string;
            case 2:
                return loadEmbeddedString();
            default:
                throw new Error("Expected 0 or 1 but was " + buffer.get());
        }
    }

    private ParseResult.Comment[] loadComments() {
        int count = loadVarInt();
        ParseResult.Comment[] comments = new ParseResult.Comment[count];

        for (int i = 0; i < count; i++) {
            ParseResult.CommentType type = ParseResult.CommentType.VALUES[buffer.get()];
            Nodes.Location location = loadLocation();

            ParseResult.Comment comment = new ParseResult.Comment(type, location);
            comments[i] = comment;
        }

        return comments;
    }

    private ParseResult.Error[] loadSyntaxErrors() {
        int count = loadVarInt();
        ParseResult.Error[] errors = new ParseResult.Error[count];

        // error messages only contain ASCII characters
        for (int i = 0; i < count; i++) {
            byte[] bytes = loadEmbeddedString();
            String message = new String(bytes, StandardCharsets.US_ASCII);
            Nodes.Location location = loadLocation();

            ParseResult.Error error = new ParseResult.Error(message, location);
            errors[i] = error;
        }

        return errors;
    }

    private ParseResult.Warning[] loadWarnings() {
        int count = loadVarInt();
        ParseResult.Warning[] warnings = new ParseResult.Warning[count];

        // warning messages only contain ASCII characters
        for (int i = 0; i < count; i++) {
            byte[] bytes = loadEmbeddedString();
            String message = new String(bytes, StandardCharsets.US_ASCII);
            Nodes.Location location = loadLocation();

            ParseResult.Warning warning = new ParseResult.Warning(message, location);
            warnings[i] = warning;
        }

        return warnings;
    }

    private Nodes.Node loadOptionalNode() {
        if (buffer.get(buffer.position()) != 0) {
            return loadNode();
        } else {
            buffer.position(buffer.position() + 1); // continue after the 0 byte
            return null;
        }
    }

    private Nodes.Location[] loadLocations() {
        int length = loadVarInt();
        if (length == 0) {
            return Nodes.Location.EMPTY_ARRAY;
        }
        Nodes.Location[] locations = new Nodes.Location[length];
        for (int i = 0; i < length; i++) {
            locations[i] = loadLocation();
        }
        return locations;
    }

    private byte[] loadConstant() {
        return constantPool.get(buffer, loadVarInt());
    }

    private byte[][] loadConstants() {
        int length = loadVarInt();
        if (length == 0) {
            return Nodes.EMPTY_BYTE_ARRAY_ARRAY;
        }
        byte[][] constants = new byte[length][];
        for (int i = 0; i < length; i++) {
            constants[i] = constantPool.get(buffer, loadVarInt());
        }
        return constants;
    }

    private Nodes.Node[] loadNodes() {
        int length = loadVarInt();
        if (length == 0) {
            return Nodes.Node.EMPTY_ARRAY;
        }
        Nodes.Node[] nodes = new Nodes.Node[length];
        for (int i = 0; i < length; i++) {
            nodes[i] = loadNode();
        }
        return nodes;
    }

    private Nodes.Location loadLocation() {
        return new Nodes.Location(loadVarInt(), loadVarInt());
    }

    private Nodes.Location loadOptionalLocation() {
        if (buffer.get() != 0) {
            return loadLocation();
        } else {
            return null;
        }
    }

    // From https://github.com/protocolbuffers/protobuf/blob/v23.1/java/core/src/main/java/com/google/protobuf/BinaryReader.java#L1507
    private int loadVarInt() {
        int x;
        if ((x = buffer.get()) >= 0) {
            return x;
        } else if ((x ^= (buffer.get() << 7)) < 0) {
            x ^= (~0 << 7);
        } else if ((x ^= (buffer.get() << 14)) >= 0) {
            x ^= (~0 << 7) ^ (~0 << 14);
        } else if ((x ^= (buffer.get() << 21)) < 0) {
            x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
        } else {
            x ^= buffer.get() << 28;
            x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
        }
        return x;
    }

    private short loadFlags() {
        int flags = loadVarInt();
        assert flags >= 0 && flags <= Short.MAX_VALUE;
        return (short) flags;
    }

    private Nodes.Node loadNode() {
        int type = buffer.get() & 0xFF;
        int startOffset = loadVarInt();
        int length = loadVarInt();

        switch (type) {
            case 1:
                return new Nodes.AliasNode(loadNode(), loadNode(), loadLocation(), startOffset, length);
            case 2:
                return new Nodes.AlternationPatternNode(loadNode(), loadNode(), loadLocation(), startOffset, length);
            case 3:
                return new Nodes.AndNode(loadNode(), loadNode(), loadLocation(), startOffset, length);
            case 4:
                return new Nodes.ArgumentsNode(loadNodes(), startOffset, length);
            case 5:
                return new Nodes.ArrayNode(loadNodes(), loadOptionalLocation(), loadOptionalLocation(), startOffset, length);
            case 6:
                return new Nodes.ArrayPatternNode(loadOptionalNode(), loadNodes(), loadOptionalNode(), loadNodes(), loadOptionalLocation(), loadOptionalLocation(), startOffset, length);
            case 7:
                return new Nodes.AssocNode(loadNode(), loadOptionalNode(), loadOptionalLocation(), startOffset, length);
            case 8:
                return new Nodes.AssocSplatNode(loadOptionalNode(), loadLocation(), startOffset, length);
            case 9:
                return new Nodes.BackReferenceReadNode(startOffset, length);
            case 10:
                return new Nodes.BeginNode(loadOptionalLocation(), (Nodes.StatementsNode) loadOptionalNode(), (Nodes.RescueNode) loadOptionalNode(), (Nodes.ElseNode) loadOptionalNode(), (Nodes.EnsureNode) loadOptionalNode(), loadOptionalLocation(), startOffset, length);
            case 11:
                return new Nodes.BlockArgumentNode(loadOptionalNode(), loadLocation(), startOffset, length);
            case 12:
                return new Nodes.BlockNode(loadConstants(), (Nodes.BlockParametersNode) loadOptionalNode(), loadOptionalNode(), loadLocation(), loadLocation(), startOffset, length);
            case 13:
                return new Nodes.BlockParameterNode(loadOptionalLocation(), loadLocation(), startOffset, length);
            case 14:
                return new Nodes.BlockParametersNode((Nodes.ParametersNode) loadOptionalNode(), loadLocations(), loadOptionalLocation(), loadOptionalLocation(), startOffset, length);
            case 15:
                return new Nodes.BreakNode((Nodes.ArgumentsNode) loadOptionalNode(), loadLocation(), startOffset, length);
            case 16:
                return new Nodes.CallNode(loadOptionalNode(), loadOptionalLocation(), loadOptionalLocation(), loadOptionalLocation(), (Nodes.ArgumentsNode) loadOptionalNode(), loadOptionalLocation(), (Nodes.BlockNode) loadOptionalNode(), loadFlags(), loadString(), startOffset, length);
            case 17:
                return new Nodes.CallOperatorAndWriteNode((Nodes.CallNode) loadNode(), loadLocation(), loadNode(), startOffset, length);
            case 18:
                return new Nodes.CallOperatorOrWriteNode((Nodes.CallNode) loadNode(), loadNode(), loadLocation(), startOffset, length);
            case 19:
                return new Nodes.CallOperatorWriteNode((Nodes.CallNode) loadNode(), loadLocation(), loadNode(), loadConstant(), startOffset, length);
            case 20:
                return new Nodes.CapturePatternNode(loadNode(), loadNode(), loadLocation(), startOffset, length);
            case 21:
                return new Nodes.CaseNode(loadOptionalNode(), loadNodes(), (Nodes.ElseNode) loadOptionalNode(), loadLocation(), loadLocation(), startOffset, length);
            case 22:
                return new Nodes.ClassNode(loadConstants(), loadLocation(), loadNode(), loadOptionalLocation(), loadOptionalNode(), loadOptionalNode(), loadLocation(), loadString(), startOffset, length);
            case 23:
                return new Nodes.ClassVariableAndWriteNode(loadLocation(), loadLocation(), loadNode(), startOffset, length);
            case 24:
                return new Nodes.ClassVariableOperatorWriteNode(loadLocation(), loadLocation(), loadNode(), loadConstant(), startOffset, length);
            case 25:
                return new Nodes.ClassVariableOrWriteNode(loadLocation(), loadLocation(), loadNode(), startOffset, length);
            case 26:
                return new Nodes.ClassVariableReadNode(startOffset, length);
            case 27:
                return new Nodes.ClassVariableTargetNode(startOffset, length);
            case 28:
                return new Nodes.ClassVariableWriteNode(loadLocation(), loadOptionalNode(), loadOptionalLocation(), startOffset, length);
            case 29:
                return new Nodes.ConstantAndWriteNode(loadLocation(), loadLocation(), loadNode(), startOffset, length);
            case 30:
                return new Nodes.ConstantOperatorWriteNode(loadLocation(), loadLocation(), loadNode(), loadConstant(), startOffset, length);
            case 31:
                return new Nodes.ConstantOrWriteNode(loadLocation(), loadLocation(), loadNode(), startOffset, length);
            case 32:
                return new Nodes.ConstantPathAndWriteNode((Nodes.ConstantPathNode) loadNode(), loadLocation(), loadNode(), startOffset, length);
            case 33:
                return new Nodes.ConstantPathNode(loadOptionalNode(), loadNode(), loadLocation(), startOffset, length);
            case 34:
                return new Nodes.ConstantPathOperatorWriteNode((Nodes.ConstantPathNode) loadNode(), loadLocation(), loadNode(), loadConstant(), startOffset, length);
            case 35:
                return new Nodes.ConstantPathOrWriteNode((Nodes.ConstantPathNode) loadNode(), loadLocation(), loadNode(), startOffset, length);
            case 36:
                return new Nodes.ConstantPathTargetNode(loadOptionalNode(), loadNode(), loadLocation(), startOffset, length);
            case 37:
                return new Nodes.ConstantPathWriteNode((Nodes.ConstantPathNode) loadNode(), loadLocation(), loadNode(), startOffset, length);
            case 38:
                return new Nodes.ConstantReadNode(startOffset, length);
            case 39:
                return new Nodes.ConstantTargetNode(startOffset, length);
            case 40:
                return new Nodes.ConstantWriteNode(loadLocation(), loadNode(), loadLocation(), startOffset, length);
            case 41:
                return new Nodes.DefNode(buffer.getInt(), loadLocation(), loadOptionalNode(), (Nodes.ParametersNode) loadOptionalNode(), loadOptionalNode(), loadConstants(), loadLocation(), loadOptionalLocation(), loadOptionalLocation(), loadOptionalLocation(), loadOptionalLocation(), loadOptionalLocation(), startOffset, length);
            case 42:
                return new Nodes.DefinedNode(loadOptionalLocation(), loadNode(), loadOptionalLocation(), loadLocation(), startOffset, length);
            case 43:
                return new Nodes.ElseNode(loadLocation(), (Nodes.StatementsNode) loadOptionalNode(), loadOptionalLocation(), startOffset, length);
            case 44:
                return new Nodes.EmbeddedStatementsNode(loadLocation(), (Nodes.StatementsNode) loadOptionalNode(), loadLocation(), startOffset, length);
            case 45:
                return new Nodes.EmbeddedVariableNode(loadLocation(), loadNode(), startOffset, length);
            case 46:
                return new Nodes.EnsureNode(loadLocation(), (Nodes.StatementsNode) loadOptionalNode(), loadLocation(), startOffset, length);
            case 47:
                return new Nodes.FalseNode(startOffset, length);
            case 48:
                return new Nodes.FindPatternNode(loadOptionalNode(), loadNode(), loadNodes(), loadNode(), loadOptionalLocation(), loadOptionalLocation(), startOffset, length);
            case 49:
                return new Nodes.FlipFlopNode(loadOptionalNode(), loadOptionalNode(), loadLocation(), loadFlags(), startOffset, length);
            case 50:
                return new Nodes.FloatNode(startOffset, length);
            case 51:
                return new Nodes.ForNode(loadNode(), loadNode(), (Nodes.StatementsNode) loadOptionalNode(), loadLocation(), loadLocation(), loadOptionalLocation(), loadLocation(), startOffset, length);
            case 52:
                return new Nodes.ForwardingArgumentsNode(startOffset, length);
            case 53:
                return new Nodes.ForwardingParameterNode(startOffset, length);
            case 54:
                return new Nodes.ForwardingSuperNode((Nodes.BlockNode) loadOptionalNode(), startOffset, length);
            case 55:
                return new Nodes.GlobalVariableAndWriteNode(loadLocation(), loadLocation(), loadNode(), startOffset, length);
            case 56:
                return new Nodes.GlobalVariableOperatorWriteNode(loadLocation(), loadLocation(), loadNode(), loadConstant(), startOffset, length);
            case 57:
                return new Nodes.GlobalVariableOrWriteNode(loadLocation(), loadLocation(), loadNode(), startOffset, length);
            case 58:
                return new Nodes.GlobalVariableReadNode(startOffset, length);
            case 59:
                return new Nodes.GlobalVariableTargetNode(startOffset, length);
            case 60:
                return new Nodes.GlobalVariableWriteNode(loadLocation(), loadLocation(), loadNode(), startOffset, length);
            case 61:
                return new Nodes.HashNode(loadLocation(), loadNodes(), loadLocation(), startOffset, length);
            case 62:
                return new Nodes.HashPatternNode(loadOptionalNode(), loadNodes(), loadOptionalNode(), loadOptionalLocation(), loadOptionalLocation(), startOffset, length);
            case 63:
                return new Nodes.IfNode(loadOptionalLocation(), loadNode(), (Nodes.StatementsNode) loadOptionalNode(), loadOptionalNode(), loadOptionalLocation(), startOffset, length);
            case 64:
                return new Nodes.ImaginaryNode(loadNode(), startOffset, length);
            case 65:
                return new Nodes.InNode(loadNode(), (Nodes.StatementsNode) loadOptionalNode(), loadLocation(), loadOptionalLocation(), startOffset, length);
            case 66:
                return new Nodes.InstanceVariableAndWriteNode(loadLocation(), loadLocation(), loadNode(), startOffset, length);
            case 67:
                return new Nodes.InstanceVariableOperatorWriteNode(loadLocation(), loadLocation(), loadNode(), loadConstant(), startOffset, length);
            case 68:
                return new Nodes.InstanceVariableOrWriteNode(loadLocation(), loadLocation(), loadNode(), startOffset, length);
            case 69:
                return new Nodes.InstanceVariableReadNode(startOffset, length);
            case 70:
                return new Nodes.InstanceVariableTargetNode(startOffset, length);
            case 71:
                return new Nodes.InstanceVariableWriteNode(loadLocation(), loadNode(), loadLocation(), startOffset, length);
            case 72:
                return new Nodes.IntegerNode(startOffset, length);
            case 73:
                return new Nodes.InterpolatedRegularExpressionNode(loadLocation(), loadNodes(), loadLocation(), loadFlags(), startOffset, length);
            case 74:
                return new Nodes.InterpolatedStringNode(loadOptionalLocation(), loadNodes(), loadOptionalLocation(), startOffset, length);
            case 75:
                return new Nodes.InterpolatedSymbolNode(loadOptionalLocation(), loadNodes(), loadOptionalLocation(), startOffset, length);
            case 76:
                return new Nodes.InterpolatedXStringNode(loadLocation(), loadNodes(), loadLocation(), startOffset, length);
            case 77:
                return new Nodes.KeywordHashNode(loadNodes(), startOffset, length);
            case 78:
                return new Nodes.KeywordParameterNode(loadLocation(), loadOptionalNode(), startOffset, length);
            case 79:
                return new Nodes.KeywordRestParameterNode(loadLocation(), loadOptionalLocation(), startOffset, length);
            case 80:
                return new Nodes.LambdaNode(loadConstants(), loadLocation(), loadLocation(), loadLocation(), (Nodes.BlockParametersNode) loadOptionalNode(), loadOptionalNode(), startOffset, length);
            case 81:
                return new Nodes.LocalVariableAndWriteNode(loadLocation(), loadLocation(), loadNode(), loadConstant(), loadVarInt(), startOffset, length);
            case 82:
                return new Nodes.LocalVariableOperatorWriteNode(loadLocation(), loadLocation(), loadNode(), loadConstant(), loadConstant(), loadVarInt(), startOffset, length);
            case 83:
                return new Nodes.LocalVariableOrWriteNode(loadLocation(), loadLocation(), loadNode(), loadConstant(), loadVarInt(), startOffset, length);
            case 84:
                return new Nodes.LocalVariableReadNode(loadConstant(), loadVarInt(), startOffset, length);
            case 85:
                return new Nodes.LocalVariableTargetNode(loadConstant(), loadVarInt(), startOffset, length);
            case 86:
                return new Nodes.LocalVariableWriteNode(loadConstant(), loadVarInt(), loadNode(), loadLocation(), loadLocation(), startOffset, length);
            case 87:
                return new Nodes.MatchPredicateNode(loadNode(), loadNode(), loadLocation(), startOffset, length);
            case 88:
                return new Nodes.MatchRequiredNode(loadNode(), loadNode(), loadLocation(), startOffset, length);
            case 89:
                return new Nodes.MissingNode(startOffset, length);
            case 90:
                return new Nodes.ModuleNode(loadConstants(), loadLocation(), loadNode(), loadOptionalNode(), loadLocation(), loadString(), startOffset, length);
            case 91:
                return new Nodes.MultiWriteNode(loadNodes(), loadOptionalLocation(), loadOptionalNode(), loadOptionalLocation(), loadOptionalLocation(), startOffset, length);
            case 92:
                return new Nodes.NextNode((Nodes.ArgumentsNode) loadOptionalNode(), loadLocation(), startOffset, length);
            case 93:
                return new Nodes.NilNode(startOffset, length);
            case 94:
                return new Nodes.NoKeywordsParameterNode(loadLocation(), loadLocation(), startOffset, length);
            case 95:
                return new Nodes.NumberedReferenceReadNode(startOffset, length);
            case 96:
                return new Nodes.OptionalParameterNode(loadConstant(), loadLocation(), loadLocation(), loadNode(), startOffset, length);
            case 97:
                return new Nodes.OrNode(loadNode(), loadNode(), loadLocation(), startOffset, length);
            case 98:
                return new Nodes.ParametersNode(loadNodes(), loadNodes(), loadNodes(), (Nodes.RestParameterNode) loadOptionalNode(), loadNodes(), loadOptionalNode(), (Nodes.BlockParameterNode) loadOptionalNode(), startOffset, length);
            case 99:
                return new Nodes.ParenthesesNode(loadOptionalNode(), loadLocation(), loadLocation(), startOffset, length);
            case 100:
                return new Nodes.PinnedExpressionNode(loadNode(), loadLocation(), loadLocation(), loadLocation(), startOffset, length);
            case 101:
                return new Nodes.PinnedVariableNode(loadNode(), loadLocation(), startOffset, length);
            case 102:
                return new Nodes.PostExecutionNode((Nodes.StatementsNode) loadOptionalNode(), loadLocation(), loadLocation(), loadLocation(), startOffset, length);
            case 103:
                return new Nodes.PreExecutionNode((Nodes.StatementsNode) loadOptionalNode(), loadLocation(), loadLocation(), loadLocation(), startOffset, length);
            case 104:
                return new Nodes.ProgramNode(loadConstants(), (Nodes.StatementsNode) loadNode(), startOffset, length);
            case 105:
                return new Nodes.RangeNode(loadOptionalNode(), loadOptionalNode(), loadLocation(), loadFlags(), startOffset, length);
            case 106:
                return new Nodes.RationalNode(loadNode(), startOffset, length);
            case 107:
                return new Nodes.RedoNode(startOffset, length);
            case 108:
                return new Nodes.RegularExpressionNode(loadLocation(), loadLocation(), loadLocation(), loadString(), loadFlags(), startOffset, length);
            case 109:
                return new Nodes.RequiredDestructuredParameterNode(loadNodes(), loadLocation(), loadLocation(), startOffset, length);
            case 110:
                return new Nodes.RequiredParameterNode(loadConstant(), startOffset, length);
            case 111:
                return new Nodes.RescueModifierNode(loadNode(), loadLocation(), loadNode(), startOffset, length);
            case 112:
                return new Nodes.RescueNode(loadLocation(), loadNodes(), loadOptionalLocation(), loadOptionalNode(), (Nodes.StatementsNode) loadOptionalNode(), (Nodes.RescueNode) loadOptionalNode(), startOffset, length);
            case 113:
                return new Nodes.RestParameterNode(loadLocation(), loadOptionalLocation(), startOffset, length);
            case 114:
                return new Nodes.RetryNode(startOffset, length);
            case 115:
                return new Nodes.ReturnNode(loadLocation(), (Nodes.ArgumentsNode) loadOptionalNode(), startOffset, length);
            case 116:
                return new Nodes.SelfNode(startOffset, length);
            case 117:
                return new Nodes.SingletonClassNode(loadConstants(), loadLocation(), loadLocation(), loadNode(), loadOptionalNode(), loadLocation(), startOffset, length);
            case 118:
                return new Nodes.SourceEncodingNode(startOffset, length);
            case 119:
                return new Nodes.SourceFileNode(loadString(), startOffset, length);
            case 120:
                return new Nodes.SourceLineNode(startOffset, length);
            case 121:
                return new Nodes.SplatNode(loadLocation(), loadOptionalNode(), startOffset, length);
            case 122:
                return new Nodes.StatementsNode(loadNodes(), startOffset, length);
            case 123:
                return new Nodes.StringConcatNode(loadNode(), loadNode(), startOffset, length);
            case 124:
                return new Nodes.StringNode(loadOptionalLocation(), loadLocation(), loadOptionalLocation(), loadString(), startOffset, length);
            case 125:
                return new Nodes.SuperNode(loadLocation(), loadOptionalLocation(), (Nodes.ArgumentsNode) loadOptionalNode(), loadOptionalLocation(), (Nodes.BlockNode) loadOptionalNode(), startOffset, length);
            case 126:
                return new Nodes.SymbolNode(loadOptionalLocation(), loadOptionalLocation(), loadOptionalLocation(), loadString(), startOffset, length);
            case 127:
                return new Nodes.TrueNode(startOffset, length);
            case 128:
                return new Nodes.UndefNode(loadNodes(), loadLocation(), startOffset, length);
            case 129:
                return new Nodes.UnlessNode(loadLocation(), loadNode(), (Nodes.StatementsNode) loadOptionalNode(), (Nodes.ElseNode) loadOptionalNode(), loadOptionalLocation(), startOffset, length);
            case 130:
                return new Nodes.UntilNode(loadLocation(), loadOptionalLocation(), loadNode(), (Nodes.StatementsNode) loadOptionalNode(), loadFlags(), startOffset, length);
            case 131:
                return new Nodes.WhenNode(loadLocation(), loadNodes(), (Nodes.StatementsNode) loadOptionalNode(), startOffset, length);
            case 132:
                return new Nodes.WhileNode(loadLocation(), loadOptionalLocation(), loadNode(), (Nodes.StatementsNode) loadOptionalNode(), loadFlags(), startOffset, length);
            case 133:
                return new Nodes.XStringNode(loadLocation(), loadLocation(), loadLocation(), loadString(), startOffset, length);
            case 134:
                return new Nodes.YieldNode(loadLocation(), loadOptionalLocation(), (Nodes.ArgumentsNode) loadOptionalNode(), loadOptionalLocation(), startOffset, length);
            default:
                throw new Error("Unknown node type: " + type);
        }
    }

    private void expect(byte value) {
        byte b = buffer.get();
        if (b != value) {
            throw new Error("Expected " + value + " but was " + b + " at position " + buffer.position());
        }
    }

}
// @formatter:on

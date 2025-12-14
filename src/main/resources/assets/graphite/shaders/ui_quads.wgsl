struct Uniforms {
    projection: mat4x4<f32>,
};

struct VertexInput {
    @location(0) position: vec2<f32>,
    @location(1) uv: vec2<f32>,
    @location(2) color: vec4<f32>,
    @location(3) tex_id: f32,
};

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) color: vec4<f32>,
    @location(2) tex_id: f32,
};

@group(0) @binding(0) var<uniform> uniforms: Uniforms;
@group(0) @binding(1) var texture_sampler: sampler;
@group(0) @binding(2) var texture0: texture_2d<f32>;
@group(0) @binding(3) var texture1: texture_2d<f32>;
@group(0) @binding(4) var texture2: texture_2d<f32>;
@group(0) @binding(5) var texture3: texture_2d<f32>;
@group(0) @binding(6) var texture4: texture_2d<f32>;
@group(0) @binding(7) var texture5: texture_2d<f32>;
@group(0) @binding(8) var texture6: texture_2d<f32>;
@group(0) @binding(9) var texture7: texture_2d<f32>;
@group(0) @binding(10) var texture8: texture_2d<f32>;
@group(0) @binding(11) var texture9: texture_2d<f32>;
@group(0) @binding(12) var texture10: texture_2d<f32>;
@group(0) @binding(13) var texture11: texture_2d<f32>;
@group(0) @binding(14) var texture12: texture_2d<f32>;
@group(0) @binding(15) var texture13: texture_2d<f32>;
@group(0) @binding(16) var texture14: texture_2d<f32>;
@group(0) @binding(17) var texture15: texture_2d<f32>;

@vertex
fn vs_main(input: VertexInput) -> VertexOutput {
    var output: VertexOutput;
    output.position = uniforms.projection * vec4<f32>(input.position, 0.0, 1.0);
    output.uv = input.uv;
    output.color = input.color;
    output.tex_id = input.tex_id;
    return output;
}

@fragment
fn fs_main(input: VertexOutput) -> @location(0) vec4<f32> {
    var tex_color: vec4<f32>;

    let tex_id = i32(input.tex_id);

    switch (tex_id) {
        case 0: { tex_color = textureSample(texture0, texture_sampler, input.uv); }
        case 1: { tex_color = textureSample(texture1, texture_sampler, input.uv); }
        case 2: { tex_color = textureSample(texture2, texture_sampler, input.uv); }
        case 3: { tex_color = textureSample(texture3, texture_sampler, input.uv); }
        case 4: { tex_color = textureSample(texture4, texture_sampler, input.uv); }
        case 5: { tex_color = textureSample(texture5, texture_sampler, input.uv); }
        case 6: { tex_color = textureSample(texture6, texture_sampler, input.uv); }
        case 7: { tex_color = textureSample(texture7, texture_sampler, input.uv); }
        case 8: { tex_color = textureSample(texture8, texture_sampler, input.uv); }
        case 9: { tex_color = textureSample(texture9, texture_sampler, input.uv); }
        case 10: { tex_color = textureSample(texture10, texture_sampler, input.uv); }
        case 11: { tex_color = textureSample(texture11, texture_sampler, input.uv); }
        case 12: { tex_color = textureSample(texture12, texture_sampler, input.uv); }
        case 13: { tex_color = textureSample(texture13, texture_sampler, input.uv); }
        case 14: { tex_color = textureSample(texture14, texture_sampler, input.uv); }
        case 15: { tex_color = textureSample(texture15, texture_sampler, input.uv); }
        default: { tex_color = vec4<f32>(1.0, 1.0, 1.0, 1.0); }
    }

    return tex_color * input.color;
}

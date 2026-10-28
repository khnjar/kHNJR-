import React, { useEffect, useRef } from 'react';
import * as d3 from 'd3';
import { PhysiologicalState } from '../hooks/usePhysiologySimulation';

interface Props {
  history: PhysiologicalState[];
}

export const PhysiologyChart: React.FC<Props> = ({ history }) => {
  const svgRef = useRef<SVGSVGElement>(null);

  useEffect(() => {
    if (!svgRef.current || history.length < 2) return;

    const svg = d3.select(svgRef.current);
    svg.selectAll("*").remove();

    const width = 400;
    const height = 200;
    const margin = { top: 20, right: 20, bottom: 30, left: 40 };

    const x = d3.scaleLinear().domain([0, history.length - 1]).range([margin.left, width - margin.right]);
    const y = d3.scaleLinear().domain([40, 200]).range([height - margin.bottom, margin.top]);

    const line = d3.line<PhysiologicalState>()
      .x((_, i) => x(i))
      .y(d => y(d.heartRate));

    svg.append("path")
      .datum(history)
      .attr("fill", "none")
      .attr("stroke", "red")
      .attr("stroke-width", 2)
      .attr("d", line);

  }, [history]);

  return <svg ref={svgRef} width="400" height="200" />;
};
